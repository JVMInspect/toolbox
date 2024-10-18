package land.src.jvmtb.remote.impl

import com.sun.jna.Pointer
import land.src.jvmtb.remote.RemoteLibrary
import land.src.jvmtb.remote.RemoteProcess
import land.src.jvmtb.remote.RemoteProcessList
import land.src.jvmtb.remote.RemoteUnsafe

class LinuxRemoteProcess : RemoteProcess {
    override val pid: Int = 0
    override val unsafe = RemoteUnsafe(this)

    override fun read(src: Pointer, dst: Pointer, size: Int): Int {
        TODO("Not yet implemented")
    }

    override fun write(dst: Pointer, src: Pointer, size: Int): Int {
        TODO("Not yet implemented")
    }

    override fun findLibrary(library: String): RemoteLibrary? {
        TODO("Not yet implemented")
    }

    override fun is64Bit(): Boolean {
        TODO("Not yet implemented")
    }

    companion object : RemoteProcessList {
        override val remotes: Set<RemoteProcess>
            get() = TODO("Not yet implemented")
    }
}