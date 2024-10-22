package land.src.toolbox.remote.impl

import com.sun.jna.Pointer
import land.src.toolbox.remote.RemoteLibrary
import land.src.toolbox.util.LinkMap
import land.src.toolbox.util.Linux
import land.src.toolbox.util.address

private val LibC = Linux.LibC

class LinuxRemoteLibrary(private val handle: LinkMap, private val base: Long) : RemoteLibrary {
    override fun findProcedure(procedure: String): Pointer? {
        val symbol = LibC.dlsym(handle.pointer, procedure) ?: return null

        val offset = symbol.address - handle.l_addr
        return Pointer.createConstant(base + offset)
    }
}