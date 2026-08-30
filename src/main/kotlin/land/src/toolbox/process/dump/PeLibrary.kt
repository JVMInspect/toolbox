package land.src.toolbox.process.dump

import com.sun.jna.Pointer
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** One section of a PE image, in image relative (RVA) coordinates. */
class PeSection(
    val name: String,
    val virtualAddress: Long,
    val virtualSize: Long,
    val rawAddress: Long,
    val rawSize: Long,
    val characteristics: Int
) {
    /** How much address space the section occupies once mapped. */
    val span = maxOf(virtualSize, rawSize)

    operator fun contains(rva: Long) = rva >= virtualAddress && rva < virtualAddress + span
}

/**
 * A PE image (`jvm.dll`) on disk, viewed as if it were mapped into the dumped process at [base].
 *
 * Symbols are resolved through the export directory, which is how [land.src.toolbox.jvm.Symbols]
 * finds `gHotSpotVMStructs` on a live Windows process too — HotSpot exports the whole VM structs
 * table specifically so that out of process tooling can read it.
 */
class PeLibrary(override val file: File, override var base: Long = 0) : DumpImage {
    private val raf = RandomAccessFile(file, "r")
    private val channel: FileChannel = raf.channel

    private val ntHeaderOffset: Long
    private val sectionsOffset: Long
    private val sectionCount: Int

    /** `IMAGE_FILE_HEADER.TimeDateStamp`, half of a module's debug identity. */
    val timeDateStamp: Int

    /** `IMAGE_OPTIONAL_HEADER.SizeOfImage`, the other half. */
    val sizeOfImage: Long

    val is64Bit: Boolean

    private val exportDirectory: Long
    private val exportDirectorySize: Long

    init {
        val dos = buffer(readFile(0, 64))
        require(dos.getShort(0) == MZ) { "${file.name} is not a PE image" }

        ntHeaderOffset = dos.getInt(0x3c).toLong() and 0xffffffffL

        val headers = buffer(readFile(ntHeaderOffset, 24 + MAX_OPTIONAL_HEADER))
        require(headers.getInt(0) == PE_SIGNATURE) { "${file.name} has no PE signature" }

        timeDateStamp = headers.getInt(8)

        val optionalHeaderSize = headers.getShort(20).toInt() and 0xffff
        sectionCount = headers.getShort(6).toInt() and 0xffff
        sectionsOffset = ntHeaderOffset + 24 + optionalHeaderSize

        val optional = 24
        is64Bit = when (val magic = headers.getShort(optional).toInt() and 0xffff) {
            0x20b -> true
            0x10b -> false
            else -> error("Unknown PE optional header magic 0x${magic.toString(16)}")
        }

        sizeOfImage = headers.getInt(optional + 56).toLong() and 0xffffffffL

        // The data directory follows the fixed part of the optional header, whose length differs
        // between PE32 and PE32+; entry zero is the export directory.
        val dataDirectory = optional + if (is64Bit) 112 else 96
        exportDirectory = headers.getInt(dataDirectory).toLong() and 0xffffffffL
        exportDirectorySize = headers.getInt(dataDirectory + 4).toLong() and 0xffffffffL
    }

    val sections: List<PeSection> by lazy {
        val table = buffer(readFile(sectionsOffset, sectionCount * 40))

        (0 until sectionCount).map { index ->
            val entry = index * 40
            val name = ByteArray(8).also { table.position(entry); table.get(it) }

            PeSection(
                String(name).trimEnd('\u0000', ' '),
                table.getInt(entry + 12).toLong() and 0xffffffffL,
                table.getInt(entry + 8).toLong() and 0xffffffffL,
                table.getInt(entry + 20).toLong() and 0xffffffffL,
                table.getInt(entry + 16).toLong() and 0xffffffffL,
                table.getInt(entry + 36)
            )
        }
    }

    override val segments by lazy {
        sections.map { ImageSegment(it.virtualAddress, it.span, it.characteristics) }
            .sortedBy { it.virtualAddress }
    }

    override val start get() = base
    override val end get() = base + sizeOfImage

    /** Timestamp and image size, the pair Windows debuggers use to identify a module build. */
    override val signature: ByteArray by lazy {
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(timeDateStamp)
            .putInt(sizeOfImage.toInt())
            .array()
    }

    private val exports: Map<String, Long> by lazy {
        if (exportDirectory == 0L || exportDirectorySize == 0L) return@lazy emptyMap()

        val directory = buffer(readRva(exportDirectory, 40) ?: return@lazy emptyMap())

        val functionCount = directory.getInt(20).toLong() and 0xffffffffL
        val nameCount = directory.getInt(24).toLong() and 0xffffffffL
        val functions = directory.getInt(28).toLong() and 0xffffffffL
        val names = directory.getInt(32).toLong() and 0xffffffffL
        val ordinals = directory.getInt(36).toLong() and 0xffffffffL

        if (nameCount <= 0 || nameCount > MAX_EXPORTS) return@lazy emptyMap()

        val nameTable = buffer(readRva(names, (nameCount * 4).toInt()) ?: return@lazy emptyMap())
        val ordinalTable = buffer(readRva(ordinals, (nameCount * 2).toInt()) ?: return@lazy emptyMap())
        val functionTable = buffer(readRva(functions, (functionCount * 4).toInt()) ?: return@lazy emptyMap())

        val result = HashMap<String, Long>(nameCount.toInt())

        for (index in 0 until nameCount.toInt()) {
            val ordinal = ordinalTable.getShort(index * 2).toInt() and 0xffff
            if (ordinal >= functionCount) continue

            val nameRva = nameTable.getInt(index * 4).toLong() and 0xffffffffL
            val name = readStringRva(nameRva) ?: continue

            result[name] = functionTable.getInt(ordinal * 4).toLong() and 0xffffffffL
        }

        result
    }

    override fun findProcedure(procedure: String): Pointer? {
        val rva = exports[procedure] ?: return null

        // A forwarded export points back into the export directory instead of at real code. Nothing
        // the VM structs walk needs one, so treat it as not found rather than handing back garbage.
        if (rva >= exportDirectory && rva < exportDirectory + exportDirectorySize) return null

        return Pointer.createConstant(base + rva)
    }

    override fun read(address: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        val rva = address - base
        if (rva < 0 || rva >= sizeOfImage) return 0

        val section = sections.firstOrNull { rva in it }
            ?: return 0

        val offsetInSection = rva - section.virtualAddress
        val wanted = minOf(length.toLong(), section.virtualAddress + section.span - rva).toInt()

        // Past SizeOfRawData the section is uninitialised data, zero filled by the loader.
        val fromFile = minOf(wanted.toLong(), maxOf(0, section.rawSize - offsetInSection)).toInt()

        if (fromFile > 0) {
            val bytes = readFile(section.rawAddress + offsetInSection, fromFile)
            System.arraycopy(bytes, 0, destination, destinationOffset, fromFile)
        }
        if (fromFile < wanted) {
            destination.fill(0, destinationOffset + fromFile, destinationOffset + wanted)
        }

        return wanted
    }

    private fun readRva(rva: Long, length: Int): ByteArray? {
        val section = sections.firstOrNull { rva in it }
            ?: return null

        val offsetInSection = rva - section.virtualAddress
        if (offsetInSection + length > section.rawSize) return null

        return readFile(section.rawAddress + offsetInSection, length)
    }

    private fun readStringRva(rva: Long): String? {
        val section = sections.firstOrNull { rva in it }
            ?: return null

        val offsetInSection = rva - section.virtualAddress
        val available = minOf(MAX_EXPORT_NAME.toLong(), section.rawSize - offsetInSection).toInt()
        if (available <= 0) return null

        val bytes = readFile(section.rawAddress + offsetInSection, available)
        val end = bytes.indexOf(0.toByte()).let { if (it < 0) bytes.size else it }

        return String(bytes, 0, end)
    }

    private fun readFile(offset: Long, length: Int): ByteArray {
        val buffer = ByteBuffer.allocate(length)

        var position = offset
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer, position)
            if (read <= 0) break
            position += read
        }

        return buffer.array()
    }

    private fun buffer(bytes: ByteArray): ByteBuffer =
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    override fun close() = raf.close()

    override fun toString() = "${file.name} @ 0x${base.toString(16)}"

    companion object {
        const val MZ: Short = 0x5a4d
        const val PE_SIGNATURE = 0x00004550

        private const val MAX_OPTIONAL_HEADER = 240
        private const val MAX_EXPORTS = 1 shl 20
        private const val MAX_EXPORT_NAME = 512
    }
}
