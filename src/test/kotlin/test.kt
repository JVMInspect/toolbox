import land.src.toolbox.jvm.VMFlags
import land.src.toolbox.jvm.VirtualMachine
import land.src.toolbox.jvm.util.ICONST_0
import land.src.toolbox.jvm.util.ICONST_1
import land.src.toolbox.jvm.util.ICONST_2
import land.src.toolbox.jvm.util.IRETURN
//import land.src.toolbox.local.mirror
import land.src.toolbox.process.impl.LinuxProcessHandle
import land.src.toolbox.process.impl.WindowsProcessHandle

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
    val method = testClass123Klass.findMethod("a", "()I")!!

    var count = 0
    println("Making method hot")
    while (method.compiledMethod == null) {
        testString.a()
    }

    println("_code is: ${method.compiledMethod!!.base.toHexString()}")

    println("a() = ${testString.a()}");

    {
        println("Deoptimizing method")
        // alter bytecode
        method.constMethod.code[0] = ICONST_0.toByte()
        method.constMethod.code[1] = IRETURN.toByte()
        //method.unsafe.putByte(method.constMethod.codeAddress.base, ICONST_0.toByte())
        //method.unsafe.putByte(method.constMethod.codeAddress.base + 1, IRETURN.toByte())
        method.deoptimizie()
    }() // magic

    println("a() = ${testString.a()}");

    println("_code is: ${method.compiledMethod}")

    println("Making method hot")
    count = 0
    while (method.compiledMethod == null) {
        testString.a()
        count++
    }

    println("_code is: ${method.compiledMethod!!.base.toHexString()}")

    count = 10
    while (count-- > 0) {
        Thread.sleep(100)
        System.gc()
    }

    println("a() = ${testString.a()}");

    proc.detach()
}