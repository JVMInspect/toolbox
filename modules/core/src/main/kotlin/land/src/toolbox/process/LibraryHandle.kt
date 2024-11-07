package land.src.toolbox.process

import com.sun.jna.Pointer

interface LibraryHandle {
    fun findProcedure(procedure: String): Pointer?
}