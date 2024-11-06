package land.src.toolbox.process

import com.sun.jna.Pointer

interface ProcessHandle {
    val pid: Int
    val local: Boolean
    val unsafe: ProcessUnsafe

    fun attach() = Unit
    fun detach() = Unit

    fun is64Bit(): Boolean

    fun read(src: Pointer, dst: Pointer, size: Int): Int
    fun write(dst: Pointer, src: Pointer, size: Int): Int

    fun findLibrary(library: String): LibraryHandle?

    fun allocate(size: Long, prot: Int = 0): Long = unsafe.allocateMemory0(size, prot)
}