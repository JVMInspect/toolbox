package land.src.toolbox.process.impl

import com.sun.jna.Pointer
import land.src.toolbox.process.LibraryHandle
import land.src.toolbox.util.LinkMap
import land.src.toolbox.util.Linux
import land.src.toolbox.util.address

private val LibC = Linux.LibC

class LinuxLibraryHandle(private val handle: LinkMap, private val base: Long) : LibraryHandle {
    override fun findProcedure(procedure: String): Pointer? {
        val symbol = LibC.dlsym(handle.pointer, procedure) ?: return null

        val offset = symbol.address - handle.l_addr
        return Pointer.createConstant(base + offset)
    }
}