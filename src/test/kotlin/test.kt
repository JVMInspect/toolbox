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

    println(testClass123Klass.constantPool.address.base)

    testString.test()

    codeReplacer.replace(newClassFile)

    testString.test()

    println(testClass123Klass.constantPool.address.base)

    val outFile = File("testOut.class")
    val outFileStream = DataOutputStream(outFile.outputStream())
    val dumper = KlassDumper(vm, testClass123Klass, outFileStream)
    dumper.writeClassFileFormat()

    outFileStream.flush()
    outFileStream.close()

    proc.detach()
}