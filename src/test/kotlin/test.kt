//import land.src.toolbox.local.mirror
import land.src.toolbox.jvm.VMFlags
import land.src.toolbox.jvm.VirtualMachine
import land.src.toolbox.jvm.util.CodeReplacer
import land.src.toolbox.jvm.util.DynamicTypeResolver
import land.src.toolbox.jvm.util.JVM_CONSTANT_Utf8
import land.src.toolbox.jvm.util.KlassDumper
import land.src.toolbox.jvm.util.T_INT
import land.src.toolbox.jvm.util.toRealObject
import land.src.toolbox.jvm.util.toVMObject
import land.src.toolbox.process.impl.LinuxProcessHandle
import land.src.toolbox.process.impl.WindowsProcessHandle
import sun.misc.Unsafe
import java.io.DataOutputStream
import java.io.File
import kotlin.jvm.javaClass

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val proc = System.getProperty("os.name").let {
        when {
            it.contains("linux", ignoreCase = true) -> LinuxProcessHandle.current
            it.contains("windows", ignoreCase = true) -> WindowsProcessHandle.current
            else -> error("Unsupported OS: $it")
        }
    }

    val realUnsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
    realUnsafeField.isAccessible = true
    val unsafe = realUnsafeField.get(null) as Unsafe

    proc.attach()
    val vm = VirtualMachine(proc)

    vm.print()

    proc.detach()
}