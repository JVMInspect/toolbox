package land.src.toolbox.process.dump

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** A range of process memory stored in the dump, together with where its bytes live in the file. */
class DumpRange(val start: Long, val size: Long, val fileOffset: Long) {
    val end get() = start + size

    operator fun contains(address: Long) = address >= start && address < end

    override fun toString() =
        "range(0x${start.toString(16)}-0x${end.toString(16)})"
}

/**
 * Read access to a Windows minidump of a JVM process.
 *
 * A dump written with `MiniDumpWithFullMemory` (`procdump -ma`, Task Manager's "Create dump file")
 * carries every committed page and is self sufficient. Smaller dumps only carry thread stacks and
 * a module list, in which case the module files on disk supply the rest — the same arrangement the
 * ELF path needs, for the same reason.
 */
class MiniDump(override val file: File) : DumpSource {
    private val raf = RandomAccessFile(file, "r")
    private val channel: FileChannel = raf.channel

    private val streams: Map<Int, DumpRange>

    override val is64Bit: Boolean

    /** Ranges of stored memory, sorted by address so lookups can binary search. */
    val ranges: List<DumpRange>

    init {
        val header = buffer(readAt(0, 32))
        require(header.getInt(0) == SIGNATURE) { "${file.name} is not a minidump" }

        val streamCount = header.getInt(8)
        val directoryOffset = header.getInt(12).toLong() and 0xffffffffL

        require(streamCount in 0..MAX_STREAMS) { "Implausible minidump stream count $streamCount" }

        val directory = buffer(readAt(directoryOffset, streamCount * 12))
        streams = (0 until streamCount).associate { index ->
            val entry = index * 12
            directory.getInt(entry) to DumpRange(
                0,
                directory.getInt(entry + 4).toLong() and 0xffffffffL,
                directory.getInt(entry + 8).toLong() and 0xffffffffL
            )
        }

        is64Bit = readArchitecture()
        ranges = readRanges()
    }

    private fun readArchitecture(): Boolean {
        val stream = streams[SYSTEM_INFO_STREAM] ?: return true
        val info = buffer(readAt(stream.fileOffset, 2))

        return when (val architecture = info.getShort(0).toInt() and 0xffff) {
            PROCESSOR_ARCHITECTURE_INTEL -> false
            PROCESSOR_ARCHITECTURE_AMD64, PROCESSOR_ARCHITECTURE_ARM64 -> true
            else -> error("Unsupported minidump processor architecture $architecture")
        }
    }

    private fun readRanges(): List<DumpRange> {
        // Memory64List is what a full memory dump uses: descriptors first, then all of the payload
        // packed back to back starting at a single base RVA.
        streams[MEMORY64_LIST_STREAM]?.let { stream ->
            val head = buffer(readAt(stream.fileOffset, 16))
            val count = head.getLong(0)
            val baseRva = head.getLong(8)

            if (count <= 0 || count > MAX_RANGES) return emptyList()

            val descriptors = buffer(readAt(stream.fileOffset + 16, (count * 16).toInt()))
            var payload = baseRva

            return (0 until count.toInt()).map { index ->
                val start = descriptors.getLong(index * 16)
                val size = descriptors.getLong(index * 16 + 8)
                DumpRange(start, size, payload).also { payload += size }
            }.sortedBy { it.start }
        }

        // The plain MemoryList carries an explicit RVA per range instead.
        streams[MEMORY_LIST_STREAM]?.let { stream ->
            val head = buffer(readAt(stream.fileOffset, 4))
            val count = head.getInt(0)

            if (count <= 0 || count > MAX_RANGES) return emptyList()

            val descriptors = buffer(readAt(stream.fileOffset + 4, count * 16))

            return (0 until count).map { index ->
                val entry = index * 16
                DumpRange(
                    descriptors.getLong(entry),
                    descriptors.getInt(entry + 8).toLong() and 0xffffffffL,
                    descriptors.getInt(entry + 12).toLong() and 0xffffffffL
                )
            }.sortedBy { it.start }
        }

        return emptyList()
    }

    override val modules: List<DumpModule> by lazy {
        val stream = streams[MODULE_LIST_STREAM] ?: return@lazy emptyList()

        val head = buffer(readAt(stream.fileOffset, 4))
        val count = head.getInt(0)

        if (count <= 0 || count > MAX_MODULES) return@lazy emptyList()

        val modules = buffer(readAt(stream.fileOffset + 4, count * MODULE_SIZE))

        (0 until count).map { index ->
            val entry = index * MODULE_SIZE
            val base = modules.getLong(entry)
            val size = modules.getInt(entry + 8).toLong() and 0xffffffffL
            val nameRva = modules.getInt(entry + 20).toLong() and 0xffffffffL

            DumpModule(readMinidumpString(nameRva), base, size)
        }.sortedBy { it.base }
    }

    /** Timestamp and image size of a module as recorded in the dump, matching [PeLibrary.signature]. */
    fun moduleSignature(module: DumpModule): ByteArray? {
        val stream = streams[MODULE_LIST_STREAM] ?: return null

        val head = buffer(readAt(stream.fileOffset, 4))
        val count = head.getInt(0)
        if (count <= 0 || count > MAX_MODULES) return null

        val modules = buffer(readAt(stream.fileOffset + 4, count * MODULE_SIZE))

        for (index in 0 until count) {
            val entry = index * MODULE_SIZE
            if (modules.getLong(entry) != module.base) continue

            return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(modules.getInt(entry + 16))
                .putInt(modules.getInt(entry + 8))
                .array()
        }

        return null
    }

    override val pid: Int by lazy {
        val stream = streams[MISC_INFO_STREAM] ?: return@lazy -1
        if (stream.size < 12) return@lazy -1

        val info = buffer(readAt(stream.fileOffset, 12))
        if (info.getInt(4) and MISC1_PROCESS_ID == 0) return@lazy -1

        info.getInt(8)
    }

    private fun readMinidumpString(rva: Long): String {
        if (rva == 0L) return ""

        val length = buffer(readAt(rva, 4)).getInt(0)
        if (length <= 0 || length > MAX_STRING) return ""

        return String(readAt(rva + 4, length), Charsets.UTF_16LE)
    }

    // A PE image is not mapped linearly — sections are aligned differently in the file than in
    // memory — so modules are served through PeLibrary rather than as raw file mappings.
    override val fileMappings: List<MappedFileEntry> get() = emptyList()

    override val mappingStarts: Sequence<Long>
        get() = ranges.asSequence().map { it.start }

    /** The stored range holding [address], or `null` when the dump does not cover it. */
    fun rangeOf(address: Long): DumpRange? {
        var low = 0
        var high = ranges.size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val range = ranges[mid]

            when {
                address < range.start -> high = mid - 1
                address >= range.end -> low = mid + 1
                else -> return range
            }
        }

        return null
    }

    override fun read(address: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        val range = rangeOf(address) ?: return 0

        val offsetInRange = address - range.start
        val available = minOf(length.toLong(), range.size - offsetInRange)
        if (available <= 0) return 0

        return readCached(range.fileOffset + offsetInRange, destination, destinationOffset, available.toInt())
    }

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
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    override fun close() {
        pageCache.clear()
        raf.close()
    }

    override fun toString() =
        "MiniDump(${file.name}, pid=$pid, ${ranges.size} ranges, ${modules.size} modules)"

    companion object {
        const val SIGNATURE = 0x504d444d // 'MDMP'

        const val THREAD_LIST_STREAM = 3
        const val MODULE_LIST_STREAM = 4
        const val MEMORY_LIST_STREAM = 5
        const val SYSTEM_INFO_STREAM = 7
        const val MEMORY64_LIST_STREAM = 9
        const val MISC_INFO_STREAM = 15

        const val PROCESSOR_ARCHITECTURE_INTEL = 0
        const val PROCESSOR_ARCHITECTURE_ARM64 = 12
        const val PROCESSOR_ARCHITECTURE_AMD64 = 9

        private const val MISC1_PROCESS_ID = 1
        private const val MODULE_SIZE = 108

        private const val MAX_STREAMS = 4096
        private const val MAX_RANGES = 1 shl 22
        private const val MAX_MODULES = 1 shl 16
        private const val MAX_STRING = 1 shl 16

        private const val PAGE_SIZE = 4096
        private const val CACHED_PAGES = 4096

        /** Whether [file] starts with the minidump magic. */
        fun isMiniDump(file: File): Boolean = runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                ByteBuffer.wrap(magic).order(ByteOrder.LITTLE_ENDIAN).int == SIGNATURE
            }
        }.getOrDefault(false)
    }
}
