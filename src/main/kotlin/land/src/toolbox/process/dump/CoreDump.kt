package land.src.toolbox.process.dump

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * A loadable segment of the dump. [fileSize] may be smaller than [memorySize] (or zero) when the
 * kernel decided not to write the pages out, which is the normal case for clean file backed
 * mappings such as the text and rodata of `libjvm.so`.
 */
class DumpSegment(
    val type: Int,
    val flags: Int,
    val fileOffset: Long,
    val virtualAddress: Long,
    val fileSize: Long,
    val memorySize: Long
) {
    val end get() = virtualAddress + memorySize

    /** Whether the pages of this segment were actually written into the dump. */
    val present get() = fileSize > 0

    operator fun contains(address: Long) =
        address >= virtualAddress && address < end

    override fun toString() =
        "segment(vaddr=0x${virtualAddress.toString(16)}, memsz=0x${memorySize.toString(16)}, " +
            "filesz=0x${fileSize.toString(16)})"

    companion object {
        const val PT_LOAD = 1
        const val PT_NOTE = 4
    }
}

/** A single entry of a `PT_NOTE` segment. */
class DumpNote(val name: String, val type: Int, val description: ByteArray)

/**
 * One file backed mapping as recorded by the kernel in the `NT_FILE` note. The dump only stores the
 * mapping, never the contents, so reading these ranges requires the original file on disk.
 */
class MappedFileEntry(
    val path: String,
    val start: Long,
    val end: Long,
    val fileOffset: Long
) {
    val name: String get() = path.substringAfterLast('/')

    operator fun contains(address: Long) =
        address >= start && address < end

    override fun toString() =
        "$path @ 0x${start.toString(16)}-0x${end.toString(16)} (+0x${fileOffset.toString(16)})"
}

/**
 * Read access to an ELF core dump ("heap dump") of a crashed or `gcore`d JVM.
 *
 * The ELF header and the program headers are parsed by hand rather than through jelf because core
 * dumps regularly exceed the 2GiB a [java.nio.MappedByteBuffer] can address, and jelf insists on
 * having the whole file available at once.
 */
class CoreDump(override val file: File) : DumpSource {
    private val raf = RandomAccessFile(file, "r")
    private val channel: FileChannel = raf.channel

    override val is64Bit: Boolean
    private val order: ByteOrder

    val segments: List<DumpSegment>

    /** `PT_LOAD` segments, sorted by virtual address so lookups can binary search. */
    val loadSegments: List<DumpSegment>

    val notes: List<DumpNote> by lazy { readNotes() }

    /** File backed mappings from the `NT_FILE` note, sorted by start address. */
    val mappedFiles: List<MappedFileEntry> by lazy { readMappedFiles() }

    override val fileMappings: List<MappedFileEntry> get() = mappedFiles

    /**
     * Loaded modules, folded out of [mappedFiles]. The kernel records one entry per mapping, so the
     * several segments a shared object is split into are merged back into a single module here.
     */
    override val modules: List<DumpModule> by lazy {
        mappedFiles.groupBy { it.path }
            .map { (path, entries) ->
                val base = entries.minOf { it.start }
                DumpModule(path, base, entries.maxOf { it.end } - base)
            }
            .sortedBy { it.base }
    }

    init {
        val ident = readAt(0, 16)
        require(ident[0] == 0x7f.toByte() && ident[1] == 'E'.code.toByte() &&
                ident[2] == 'L'.code.toByte() && ident[3] == 'F'.code.toByte()) {
            "${file.name} is not an ELF file"
        }

        is64Bit = when (ident[4].toInt()) {
            2 -> true
            1 -> false
            else -> error("Unknown ELF class ${ident[4]}")
        }
        order = when (ident[5].toInt()) {
            1 -> ByteOrder.LITTLE_ENDIAN
            2 -> ByteOrder.BIG_ENDIAN
            else -> error("Unknown ELF data encoding ${ident[5]}")
        }

        val header = buffer(readAt(0, if (is64Bit) 64 else 52))
        header.position(16)

        val type = header.short.toInt() and 0xffff
        check(type == ET_CORE) {
            "${file.name} is not a core dump (e_type = $type)"
        }

        header.short // e_machine
        header.int // e_version

        val programHeaderOffset: Long

        if (is64Bit) {
            header.long // e_entry
            programHeaderOffset = header.long
            header.long // e_shoff
        } else {
            header.int // e_entry
            programHeaderOffset = header.int.toLong() and 0xffffffffL
            header.int // e_shoff
        }

        header.int // e_flags
        header.short // e_ehsize

        val programHeaderSize = header.short.toInt() and 0xffff
        val programHeaderCount = header.short.toInt() and 0xffff

        segments = readProgramHeaders(programHeaderOffset, programHeaderCount, programHeaderSize)
        loadSegments = segments
            .filter { it.type == DumpSegment.PT_LOAD }
            .sortedBy { it.virtualAddress }
    }

    private fun readProgramHeaders(offset: Long, count: Int, size: Int): List<DumpSegment> {
        if (count == 0) return emptyList()

        val table = buffer(readAt(offset, count * size))

        return (0 until count).map { index ->
            table.position(index * size)

            if (is64Bit) {
                val type = table.int
                val flags = table.int
                val fileOffset = table.long
                val virtualAddress = table.long
                table.long // p_paddr
                val fileSize = table.long
                val memorySize = table.long
                DumpSegment(type, flags, fileOffset, virtualAddress, fileSize, memorySize)
            } else {
                val type = table.int
                val fileOffset = table.int.toLong() and 0xffffffffL
                val virtualAddress = table.int.toLong() and 0xffffffffL
                table.int // p_paddr
                val fileSize = table.int.toLong() and 0xffffffffL
                val memorySize = table.int.toLong() and 0xffffffffL
                val flags = table.int
                DumpSegment(type, flags, fileOffset, virtualAddress, fileSize, memorySize)
            }
        }
    }

    private fun readNotes(): List<DumpNote> =
        segments.filter { it.type == DumpSegment.PT_NOTE }
            .flatMap { parseNotes(readAt(it.fileOffset, it.fileSize.toInt()), order) }

    private fun readMappedFiles(): List<MappedFileEntry> {
        val note = notes.firstOrNull { it.type == NT_FILE } ?: return emptyList()
        val data = buffer(note.description)

        val wordSize = if (is64Bit) 8 else 4
        fun word() = if (is64Bit) data.long else data.int.toLong() and 0xffffffffL

        if (data.remaining() < wordSize * 2) return emptyList()

        val count = word().toInt()
        val pageSize = word()

        if (count <= 0 || data.remaining() < count.toLong() * wordSize * 3) return emptyList()

        val ranges = (0 until count).map { Triple(word(), word(), word() * pageSize) }

        return ranges.map { (start, end, offset) ->
            val path = StringBuilder()
            while (data.hasRemaining()) {
                val byte = data.get()
                if (byte == 0.toByte()) break
                path.append(byte.toInt().toChar())
            }
            MappedFileEntry(path.toString(), start, end, offset)
        }.sortedBy { it.start }
    }

    /** The pid the dump was taken from, read from the `NT_PRPSINFO` note. */
    override val pid: Int by lazy {
        val note = notes.firstOrNull { it.type == NT_PRPSINFO } ?: return@lazy -1
        val offset = if (is64Bit) 24 else 12
        if (note.description.size < offset + 4) return@lazy -1
        buffer(note.description).getInt(offset)
    }

    override val mappingStarts: Sequence<Long>
        get() = loadSegments.asSequence().filter { it.present }.map { it.virtualAddress }

    /** The segment holding [address], or `null` when the address was never mapped. */
    fun segmentOf(address: Long): DumpSegment? {
        var low = 0
        var high = loadSegments.size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val segment = loadSegments[mid]

            when {
                address < segment.virtualAddress -> high = mid - 1
                address >= segment.end -> low = mid + 1
                else -> return segment
            }
        }

        return null
    }

    /**
     * Copies up to [length] bytes at [address] into [destination].
     *
     * Returns the number of bytes actually available, which is zero when the address is not covered
     * by the dump at all and may be short when the range runs past the written part of a segment.
     * The caller is expected to fall back to the file backing that range in that case.
     */
    override fun read(address: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        val segment = segmentOf(address) ?: return 0
        if (!segment.present) return 0

        val offsetInSegment = address - segment.virtualAddress
        val available = minOf(length.toLong(), segment.fileSize - offsetInSegment)
        if (available <= 0) return 0

        return readCached(segment.fileOffset + offsetInSegment, destination, destinationOffset, available.toInt())
    }

    // A small page cache keeps ProcessUnsafe.getString, which walks memory one byte at a time, from
    // turning into one pread per character.
    private val pageCache = object : LinkedHashMap<Long, ByteArray>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ByteArray>) = size > CACHED_PAGES
    }

    private fun readCached(fileOffset: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        var read = 0

        while (read < length) {
            val position = fileOffset + read
            val pageStart = position and (PAGE_SIZE - 1).inv().toLong()
            val page = pageCache.getOrPut(pageStart) { readAt(pageStart, PAGE_SIZE) }

            val offsetInPage = (position - pageStart).toInt()
            val chunk = minOf(PAGE_SIZE - offsetInPage, length - read)

            System.arraycopy(page, offsetInPage, destination, destinationOffset + read, chunk)
            read += chunk
        }

        return read
    }

    private fun readAt(offset: Long, length: Int): ByteArray {
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
        ByteBuffer.wrap(bytes).order(order)

    override fun close() {
        pageCache.clear()
        raf.close()
    }

    override fun toString() =
        "CoreDump(${file.name}, pid=$pid, ${loadSegments.size} segments, ${mappedFiles.size} mapped files)"

    companion object {
        const val ET_CORE = 4
        const val NT_PRSTATUS = 1
        const val NT_PRPSINFO = 3
        const val NT_FILE = 0x46494c45

        private const val PAGE_SIZE = 4096
        private const val CACHED_PAGES = 4096
    }
}
