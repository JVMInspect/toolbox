package land.src.toolbox.process.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A shared object whose header was found still mapped inside a dump. */
class FoundImage(
    val base: Long,
    val signature: ByteArray?,
    val segments: List<ImageSegment>
) {
    override fun toString() =
        "image @ 0x${base.toString(16)} (signature=${signature?.joinToString("") { "%02x".format(it) } ?: "none"})"
}

/**
 * Locates a library inside a dump.
 *
 * The cheap path is the module list the operating system wrote into the dump — `NT_FILE` on Linux,
 * the module list stream on Windows. When the dump has none, or its paths do not match what the
 * caller has on disk, the scanner sweeps the stored pages for image headers and matches them
 * against the supplied library by build identity, falling back to segment layout.
 *
 * The sweep works because both formats deliberately preserve image headers: the default Linux
 * `coredump_filter` (0x33) writes the first page of every file backed mapping so that debuggers can
 * recover the ELF header, and a Windows minidump stores the PE headers of every loaded module.
 */
class DumpImageScanner(private val dump: DumpSource) {

    /** The module whose name contains [nameFragment], lowest address first. */
    fun module(nameFragment: String): DumpModule? =
        dump.modules
            .filter { it.name.contains(nameFragment, ignoreCase = true) }
            .minByOrNull { it.base }

    /** Load address of a library taken from the dump's module list, or `null` if it is not listed. */
    fun findByModuleList(nameFragment: String): Long? = module(nameFragment)?.base

    /**
     * Sweeps the dump for the image of [image] and returns the address it was loaded at.
     *
     * Matches on build identity — GNU build id, or PE timestamp and image size — when both sides
     * carry one, and otherwise on segment layout, which is specific enough to tell a JVM apart from
     * the handful of other libraries in a process.
     */
    fun scanFor(image: DumpImage): Long? {
        val expectedSignature = image.signature
        val expectedSegments = image.segments.sortedBy { it.virtualAddress }

        return images().firstOrNull { found ->
            if (expectedSignature != null && found.signature != null)
                found.signature.contentEquals(expectedSignature)
            else
                matchesLayout(found.segments, expectedSegments)
        }?.base
    }

    private fun matchesLayout(found: List<ImageSegment>, expected: List<ImageSegment>): Boolean {
        if (found.isEmpty() || found.size != expected.size) return false

        return found.zip(expected).all { (a, b) ->
            a.virtualAddress == b.virtualAddress && a.size == b.size && a.flags == b.flags
        }
    }

    /** Every image header the dump still contains. Swept once and remembered. */
    fun images(): List<FoundImage> = images

    private val images: List<FoundImage> by lazy {
        dump.mappingStarts
            .mapNotNull { readElfImage(it) ?: readPeImage(it) }
            .toList()
    }

    private fun readElfImage(address: Long): FoundImage? {
        val header = readMemory(address, 64) ?: return null

        if (header[0] != 0x7f.toByte() || header[1] != 'E'.code.toByte() ||
            header[2] != 'L'.code.toByte() || header[3] != 'F'.code.toByte()) return null

        val is64Bit = header[4].toInt() == 2
        val order = if (header[5].toInt() == 2) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN

        // A dump only ever contains images of its own bitness, so a mismatch means we are looking at
        // an ELF file that happens to be mapped as data rather than at a loaded library.
        if (is64Bit != dump.is64Bit) return null

        val buffer = ByteBuffer.wrap(header).order(order)
        if ((buffer.getShort(16).toInt() and 0xffff) != ET_DYN) return null

        val programHeaderOffset: Long
        val programHeaderSize: Int
        val programHeaderCount: Int

        if (is64Bit) {
            programHeaderOffset = buffer.getLong(32)
            programHeaderSize = buffer.getShort(54).toInt() and 0xffff
            programHeaderCount = buffer.getShort(56).toInt() and 0xffff
        } else {
            programHeaderOffset = buffer.getInt(28).toLong() and 0xffffffffL
            programHeaderSize = buffer.getShort(42).toInt() and 0xffff
            programHeaderCount = buffer.getShort(44).toInt() and 0xffff
        }

        if (programHeaderCount <= 0 || programHeaderCount > MAX_PROGRAM_HEADERS) return null
        if (programHeaderSize < if (is64Bit) 56 else 32) return null

        val table = readMemory(address + programHeaderOffset, programHeaderCount * programHeaderSize)
            ?: return null
        val headers = ByteBuffer.wrap(table).order(order)

        val loads = mutableListOf<ImageSegment>()
        var noteOffset = -1L
        var noteSize = 0

        for (index in 0 until programHeaderCount) {
            val entry = index * programHeaderSize

            val type: Int
            val flags: Int
            val offset: Long
            val virtualAddress: Long
            val fileSize: Long
            val memorySize: Long

            if (is64Bit) {
                type = headers.getInt(entry)
                flags = headers.getInt(entry + 4)
                offset = headers.getLong(entry + 8)
                virtualAddress = headers.getLong(entry + 16)
                fileSize = headers.getLong(entry + 32)
                memorySize = headers.getLong(entry + 40)
            } else {
                type = headers.getInt(entry)
                offset = headers.getInt(entry + 4).toLong() and 0xffffffffL
                virtualAddress = headers.getInt(entry + 8).toLong() and 0xffffffffL
                fileSize = headers.getInt(entry + 16).toLong() and 0xffffffffL
                memorySize = headers.getInt(entry + 20).toLong() and 0xffffffffL
                flags = headers.getInt(entry + 24)
            }

            when (type) {
                PT_LOAD -> loads += ImageSegment(virtualAddress, memorySize, flags)
                PT_NOTE -> if (noteOffset < 0) {
                    noteOffset = offset
                    noteSize = fileSize.toInt()
                }
            }
        }

        if (loads.isEmpty()) return null

        // For a shared object the ELF header sits in the segment mapped at file offset zero, so the
        // address the header was found at is the load bias.
        val base = address - loads.minOf { it.virtualAddress }

        val signature = if (noteOffset >= 0 && noteSize in 1..MAX_NOTE_SIZE) {
            readMemory(base + noteOffset, noteSize)
                ?.let { parseNotes(it, order) }
                ?.firstOrNull { it.type == NT_GNU_BUILD_ID && it.name == "GNU" }
                ?.description
        } else null

        return FoundImage(base, signature, loads.sortedBy { it.virtualAddress })
    }

    private fun readPeImage(address: Long): FoundImage? {
        val dos = readMemory(address, 64) ?: return null
        val dosBuffer = ByteBuffer.wrap(dos).order(ByteOrder.LITTLE_ENDIAN)

        if (dosBuffer.getShort(0) != PeLibrary.MZ) return null

        val ntOffset = dosBuffer.getInt(0x3c).toLong() and 0xffffffffL
        if (ntOffset <= 0 || ntOffset > MAX_HEADER_SIZE) return null

        val nt = readMemory(address + ntOffset, 24 + 96) ?: return null
        val ntBuffer = ByteBuffer.wrap(nt).order(ByteOrder.LITTLE_ENDIAN)

        if (ntBuffer.getInt(0) != PeLibrary.PE_SIGNATURE) return null

        val sectionCount = ntBuffer.getShort(6).toInt() and 0xffff
        val timeDateStamp = ntBuffer.getInt(8)
        val optionalHeaderSize = ntBuffer.getShort(20).toInt() and 0xffff

        val is64Bit = when (ntBuffer.getShort(24).toInt() and 0xffff) {
            0x20b -> true
            0x10b -> false
            else -> return null
        }
        if (is64Bit != dump.is64Bit) return null

        val sizeOfImage = ntBuffer.getInt(24 + 56).toLong() and 0xffffffffL
        if (sectionCount <= 0 || sectionCount > MAX_SECTIONS) return null

        val table = readMemory(address + ntOffset + 24 + optionalHeaderSize, sectionCount * 40)
            ?: return null
        val sections = ByteBuffer.wrap(table).order(ByteOrder.LITTLE_ENDIAN)

        val segments = (0 until sectionCount).map { index ->
            val entry = index * 40
            val virtualSize = sections.getInt(entry + 8).toLong() and 0xffffffffL
            val rawSize = sections.getInt(entry + 16).toLong() and 0xffffffffL

            ImageSegment(
                sections.getInt(entry + 12).toLong() and 0xffffffffL,
                maxOf(virtualSize, rawSize),
                sections.getInt(entry + 36)
            )
        }.sortedBy { it.virtualAddress }

        val signature = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(timeDateStamp)
            .putInt(sizeOfImage.toInt())
            .array()

        return FoundImage(address, signature, segments)
    }

    private fun readMemory(address: Long, length: Int): ByteArray? {
        if (length <= 0) return null

        val bytes = ByteArray(length)
        if (dump.read(address, bytes, 0, length) != length) return null

        return bytes
    }

    companion object {
        private const val ET_DYN = 3
        private const val PT_LOAD = 1
        private const val PT_NOTE = 4

        private const val MAX_PROGRAM_HEADERS = 128
        private const val MAX_SECTIONS = 128
        private const val MAX_NOTE_SIZE = 4096
        private const val MAX_HEADER_SIZE = 4096
    }
}
