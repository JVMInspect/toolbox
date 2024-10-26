import land.src.toolbox.jvm.Universe
import land.src.toolbox.jvm.VMFlags
import land.src.toolbox.jvm.util.KlassDumper
import land.src.toolbox.jvm.VirtualMachine
import land.src.toolbox.jvm.oop.OopDesc
//import land.src.toolbox.local.mirror
import land.src.toolbox.process.impl.LinuxProcessHandle
import land.src.toolbox.process.impl.WindowsProcessHandle
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.DataOutputStream
import java.io.File

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

    //vm.print()

    val flags: VMFlags = vm.structs(-1)!!
    for (flag in flags.flags) {
        println(flag)
    }

    val universe = vm.globals.universe
    val stringKlass = universe.instanceKlass("java/lang/String")!!

    //val mirror = stringKlass.mirror
    //println(mirror)

    val fileOutput = DataOutputStream(File("String.class").outputStream())
    val klassDumper = KlassDumper(vm, stringKlass, fileOutput)

    klassDumper.writeClassFileFormat()

    ClassReader(File("String.class").readBytes())
        .accept(ClassWriter(ClassWriter.COMPUTE_FRAMES), 0)


    proc.detach()
}