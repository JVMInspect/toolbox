package land.src.toolbox.process.dump

import com.sun.jna.Pointer
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * A plain file backed mapping, served straight out of the file at the offsets the dump recorded.
 *
 * Not every mapping a dump leaves out is a library, so not every gap can be filled by a format
 * aware reader. Anything the kernel recorded a path and a file offset for can be served here
 * instead, because for a flat mapping the translation is a straight addition — no knowledge of what
 * the file contains is needed. The CDS archive is the case this exists for: HotSpot dirties most of
 * `classes.jsa` while relocating it, so a dump usually does contain it, but the regions it leaves
 * clean are only readable through the file.
 */
class MappedFileImage(override val file: File, val ranges: List<MappedFileEntry>) : DumpImage {
    private val raf = RandomAccessFile(file, "r")
    private val channel: FileChannel = raf.channel
    private val length = raf.length()

    init {
        require(ranges.isNotEmpty()) { "${file.name} has no mapped ranges" }
    }

    /** Mappings carry absolute addresses already, so there is no bias to apply. */
    override var base = 0L

    override val start = ranges.minOf { it.start }
    override val end = ranges.maxOf { it.end }

    override val signature: ByteArray? = null

    override val segments = ranges
        .map { ImageSegment(it.start, it.end - it.start, 0) }
        .sortedBy { it.virtualAddress }

    override fun findProcedure(procedure: String): Pointer? = null

    override fun read(address: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        val range = ranges.firstOrNull { address in it } ?: return 0

        val offset = range.fileOffset + (address - range.start)
        val wanted = minOf(length.toLong(), range.end - address).toInt()

        // A mapping may run past the end of the file, in which case the tail reads as zero.
        val fromFile = minOf(wanted.toLong(), maxOf(0, this.length - offset)).toInt()

        if (fromFile > 0) {
            val buffer = ByteBuffer.allocate(fromFile)

            var position = offset
            while (buffer.hasRemaining()) {
                val read = channel.read(buffer, position)
                if (read <= 0) break
                position += read
            }

            System.arraycopy(buffer.array(), 0, destination, destinationOffset, fromFile)
        }
        if (fromFile < wanted) {
            destination.fill(0, destinationOffset + fromFile, destinationOffset + wanted)
        }

        return wanted
    }

    override fun close() = raf.close()

    override fun toString() =
        "${file.name} @ 0x${start.toString(16)} (${ranges.size} mappings)"
}
