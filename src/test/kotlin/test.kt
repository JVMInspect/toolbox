//import land.src.toolbox.local.mirror
import land.src.toolbox.jvm.VMFlags
import land.src.toolbox.jvm.VirtualMachine
import land.src.toolbox.jvm.util.CodeReplacer
import land.src.toolbox.jvm.util.JVM_CONSTANT_Utf8
import land.src.toolbox.jvm.util.KlassDumper
import land.src.toolbox.process.impl.LinuxProcessHandle
import land.src.toolbox.process.impl.WindowsProcessHandle
import java.io.DataOutputStream
import java.io.File

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val proc = System.getProperty("os.name").let {
        when {
            it.contains("linux", ignoreCase = true) -> LinuxProcessHandle.current
            it.contains("windows", ignoreCase = true) -> WindowsProcessHandle.current
            else -> error("Unsupported OS: $it")
        }
    }

    proc.attach()
    val vm = VirtualMachine(proc)

    vm.print()

    val flags: VMFlags = vm.structs(-1)!!
    for (flag in flags.flags) {
        println(flag)
    }

    val testString = TestClass123()

    val universe = vm.globals.universe
    val testClass123Klass = universe.instanceKlass("TestClass123")!!

    val newClassFile = File("test.class").readBytes()
    val codeReplacer = CodeReplacer(vm, testClass123Klass.methods.find { it.name == "test" }!!)

    codeReplacer.replace(newClassFile)

    val dumper = KlassDumper(vm, testClass123Klass, DataOutputStream(File("testOut.class").outputStream()))
    dumper.writeClassFileFormat()

    proc.detach()
}