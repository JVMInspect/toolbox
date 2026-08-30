package land.src.toolbox.process.impl

import com.sun.jna.Pointer
import land.src.toolbox.process.LibraryHandle
import land.src.toolbox.process.ProcessHandle
import land.src.toolbox.process.ProcessUnsafe
import land.src.toolbox.process.dump.DumpImage
import land.src.toolbox.process.dump.DumpSource
import land.src.toolbox.util.address
import java.io.Closeable

/** Thrown when neither the dump nor any supplied library file covers an address that was read. */
class DumpReadException(val address: Long, val size: Int, val available: Int) :
    IllegalStateException(
        "Only $available of $size bytes at 0x${address.toString(16)} are present in the dump. " +
            if (address == 0L)
                "The address is null, so something the dump was supposed to contain was never set."
            else
                "Supply the library or file that was mapped there so the missing pages can be read " +
                    "from disk, or pass its load address if it was placed at the wrong one."
    )

/**
 * A read only [ProcessHandle] backed by a dump instead of a live process.
 *
 * Reads are served from the dump first and from [images] — the library files the caller supplied,
 * positioned at the address they were loaded at — for everything the dump left out. That split is
 * not an optimisation but a requirement: a Linux core dump omits clean file backed pages, so the
 * type and field name strings the `gHotSpotVMStructs` table points at exist only in `libjvm.so`
 * itself, while the table entries and every VM global live in the dumped writable pages.
 */
class HeapDumpProcessHandle(
    val dump: DumpSource,
    val images: List<DumpImage>
) : ProcessHandle, Closeable {
    override val pid = dump.pid
    override val local = false
    override val unsafe = ProcessUnsafe(this)

    override fun is64Bit() = dump.is64Bit

    override fun read(src: Pointer, dst: Pointer, size: Int): Int {
        val address = src.address
        val bytes = ByteArray(size)
        var read = 0

        // A read can cross between the dump and a file, and back again — a library's writable pages
        // are dumped while the read only ones next to them are not — so every step reconsiders both
        // sources instead of committing to whichever served the first byte.
        while (read < size) {
            val at = address + read
            var more = dump.read(at, bytes, read, size - read)

            if (more <= 0) {
                for (image in images) {
                    if (at !in image) continue

                    more = image.read(at, bytes, read, size - read)
                    if (more > 0) break
                }
            }

            if (more <= 0) break
            read += more
        }

        if (read < size) throw DumpReadException(address, size, read)

        dst.write(0, bytes, 0, size)
        return size
    }

    override fun write(dst: Pointer, src: Pointer, size: Int): Int =
        throw UnsupportedOperationException(
            "Cannot write to 0x${dst.address.toString(16)}: ${dump.file.name} is a dump, not a live process"
        )

    override fun findLibrary(library: String): LibraryHandle? {
        val requested = library.substringAfterLast('/').substringAfterLast('\\')

        images.firstOrNull { it.file.name.equals(requested, ignoreCase = true) }
            ?.let { return it }

        // Symbols asks for the platform name of the current host, which need not be the platform the
        // dump came from, so fall back to matching the stem: libjvm.so and jvm.dll both mean "jvm".
        val stem = requested
            .removeSuffix(".so").removeSuffix(".dll").removeSuffix(".dylib")
            .removePrefix("lib")

        return images.firstOrNull { it.file.name.contains(stem, ignoreCase = true) }
    }

    override fun allocate(size: Long, prot: Int): Long =
        throw UnsupportedOperationException("Cannot allocate in ${dump.file.name}: a dump is read only")

    override fun close() {
        images.forEach { it.close() }
        dump.close()
    }

    override fun toString() = "HeapDumpProcessHandle($dump, images=$images)"
}
