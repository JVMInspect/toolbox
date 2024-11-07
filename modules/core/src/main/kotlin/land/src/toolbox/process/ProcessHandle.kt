package land.src.toolbox.process

interface ProcessHandle {
    val pid: Int
    val isLocal: Boolean
    val unsafe: ProcessUnsafe

    fun attach() = Unit
    fun detach() = Unit

    fun is64Bit(): Boolean

    fun read(src: Long, dst: Long, size: Int): Int
    fun write(dst: Long, src: Long, size: Int): Int

    fun findLibrary(library: String): LibraryHandle?

    fun allocate(size: Long, prot: Int = 0): Long = unsafe.allocateMemory0(size, prot)
}