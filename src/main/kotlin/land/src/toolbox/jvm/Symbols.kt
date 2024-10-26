package land.src.toolbox.jvm

import land.src.toolbox.process.ProcessHandle
import land.src.toolbox.process.impl.WindowsProcessHandle

class Symbols(process: ProcessHandle) {
    private val libJvmName =
        if (process is WindowsProcessHandle) "jvm.dll"
        else "libjvm.so"

    private val libJvm =
        process.findLibrary(libJvmName) ?: error("$libJvmName library not found")

    fun lookup(name: String) =
        checkNotNull(libJvm.findProcedure(name)) { "Failed to find symbol $name" }
}