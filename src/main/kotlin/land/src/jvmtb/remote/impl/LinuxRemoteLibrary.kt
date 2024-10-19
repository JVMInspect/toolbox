package land.src.jvmtb.remote.impl

import com.sun.jna.Pointer
import land.src.jvmtb.remote.RemoteLibrary
import land.src.jvmtb.util.LinkMap
import land.src.jvmtb.util.Linux
import land.src.jvmtb.util.address

private val LibC = Linux.LibC

class LinuxRemoteLibrary(private val handle: LinkMap, private val base: Long) : RemoteLibrary {
    override fun findProcedure(procedure: String): Pointer? {
        val symbol = LibC.dlsym(handle.pointer, procedure) ?: return null

        val offset = symbol.address - handle.l_addr
        return Pointer.createConstant(base + offset)
    }
}