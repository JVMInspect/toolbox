package land.src.jvmtb

import land.src.jvmtb.jvm.*
import land.src.jvmtb.jvm.oop.*
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.jvm.prims.KlassDumper
import land.src.jvmtb.remote.impl.LinuxRemoteProcess
import land.src.jvmtb.remote.impl.WindowsRemoteProcess
import java.io.DataOutputStream
import java.io.File

fun main() {
    val remotes = System.getProperty("os.name").let {
        when {
            it.contains("windows", ignoreCase = true) -> setOf(WindowsRemoteProcess.current) //WindowsRemoteProcess.remotes
            it.contains("linux", ignoreCase = true) -> LinuxRemoteProcess.remotes
            else -> error("Unsupported OS: $it")
        }
    }

    val proc = remotes.first()
    proc.attach()
    val vm = VirtualMachine(proc)

    val version: VMVersion = vm.structs()
    println(version)

    vm.print()

    val universe = Universe(vm)
    val stringKlass = universe.instanceKlass("java/lang/String")!!

    val fileOutput = DataOutputStream(File("String.class").outputStream())
    val klassDumper = KlassDumper(vm, stringKlass, fileOutput)

    klassDumper.writeClassFileFormat()

    proc.detach()
}