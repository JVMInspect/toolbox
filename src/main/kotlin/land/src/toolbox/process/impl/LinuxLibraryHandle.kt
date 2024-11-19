package land.src.toolbox.process.impl

import com.sun.jna.Pointer
import land.src.toolbox.process.LibraryHandle
import land.src.toolbox.util.LinkMap
import land.src.toolbox.util.Linux
import net.fornwall.jelf.ElfFile

private val LibC = Linux.LibC

class LinuxLibraryHandle(handle: ElfFile, private val base: Long) : LibraryHandle {

    private val symbolTableSection = handle.symbolTableSection
    private val symbolMap = symbolTableSection.symbols.associateBy { it.name }

    override fun findProcedure(procedure: String): Pointer? {
        val symbol = symbolMap[procedure] ?: return null

        val offset = symbol.st_value
        return Pointer.createConstant(base + offset)
    }
}