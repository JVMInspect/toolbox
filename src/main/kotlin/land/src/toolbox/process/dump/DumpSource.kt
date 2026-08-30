package land.src.toolbox.process.dump

import land.src.toolbox.process.LibraryHandle
import java.io.Closeable
import java.io.File

/** One loadable piece of an image — a `PT_LOAD` for ELF, a section for PE — relative to its base. */
class ImageSegment(val virtualAddress: Long, val size: Long, val flags: Int) {
    override fun toString() =
        "0x${virtualAddress.toString(16)}+0x${size.toString(16)} (flags=0x${flags.toString(16)})"
}

/** A module the dump knows was loaded, as recorded by the operating system that wrote the dump. */
class DumpModule(val path: String, val base: Long, val size: Long) {
    val name: String get() = path.substringAfterLast('/').substringAfterLast('\\')

    operator fun contains(address: Long) = address >= base && address < base + size

    override fun toString() = "$path @ 0x${base.toString(16)} (0x${size.toString(16)} bytes)"
}

/**
 * A post mortem snapshot of a process' address space: an ELF core dump on Linux, a minidump on
 * Windows.
 *
 * Neither format is guaranteed to be complete. Both routinely leave out clean file backed pages, so
 * [read] is allowed to come up short and the caller is expected to fall back to the module files on
 * disk for the missing ranges.
 */
interface DumpSource : Closeable {
    val file: File

    val is64Bit: Boolean

    /** The pid the dump was taken from, or `-1` when the format did not record one. */
    val pid: Int

    /** Modules the dump lists as loaded. Empty when the dump carries no module information. */
    val modules: List<DumpModule>

    /**
     * File backed mappings with the file offsets they were mapped at, when the dump records them.
     * Lets ranges the dump omitted be read straight from the file, without knowing its format.
     */
    val fileMappings: List<MappedFileEntry>

    /**
     * Addresses at which a stored mapping begins — the only places an image header can sit, and so
     * the candidate list for [DumpImageScanner].
     */
    val mappingStarts: Sequence<Long>

    /**
     * Copies up to [length] bytes at [address] into [destination], returning how many bytes the dump
     * could actually supply. Zero means the range is not in the dump at all.
     */
    fun read(address: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int
}

/**
 * A library file on disk, viewed as if it were mapped into the dumped process at [base].
 *
 * This is what makes a partial dump usable: the pages a dump omits are exactly the ones that never
 * changed since load, so the original file still holds their contents byte for byte.
 */
interface DumpImage : LibraryHandle, Closeable {
    val file: File

    /** Load bias — a runtime address maps into the image at `address - base`. */
    var base: Long

    /** Lowest and highest runtime address the image covers. */
    val start: Long
    val end: Long

    /**
     * Build identity of this exact build: the GNU build id for ELF, the timestamp and image size for
     * PE. Used to recognise the image inside a dump.
     */
    val signature: ByteArray?

    /** Loadable pieces of the image, sorted by address, used to match it by layout alone. */
    val segments: List<ImageSegment>

    operator fun contains(address: Long) = address in start until end

    /**
     * Copies up to [length] bytes at runtime address [address] out of the file, zero filling
     * uninitialised data. Returns zero when the address is not part of this image.
     */
    fun read(address: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int
}
