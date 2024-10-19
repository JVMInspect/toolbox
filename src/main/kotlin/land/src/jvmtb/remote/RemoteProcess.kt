package land.src.jvmtb.remote

import com.sun.jna.Pointer

interface RemoteProcess {
    val pid: Int
    val unsafe: RemoteUnsafe

    fun attach() = Unit
    fun detach() = Unit

    fun is64Bit(): Boolean

    fun read(src: Pointer, dst: Pointer, size: Int): Int
    fun write(dst: Pointer, src: Pointer, size: Int): Int

    fun findLibrary(library: String): RemoteLibrary?
}