package land.src.toolbox.jvm

import land.src.toolbox.remote.RemoteProcess
import land.src.toolbox.remote.impl.WindowsRemoteProcess

class Symbols(process: RemoteProcess) {
    private val libJvmName =
        if (process is WindowsRemoteProcess) "jvm.dll"
        else "libjvm.so"

    private val libJvm =
        process.findLibrary(libJvmName) ?: error("$libJvmName library not found")

    fun lookup(name: String) =
        checkNotNull(libJvm.findProcedure(name)) { "Failed to find symbol $name" }
}