package land.src.jvmtb.remote

import com.sun.jna.Pointer

interface RemoteLibrary {
    fun findProcedure(procedure: String): Pointer?
}