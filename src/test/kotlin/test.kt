import land.src.toolbox.jvm.Universe
import land.src.toolbox.jvm.util.KlassDumper
import land.src.toolbox.jvm.VirtualMachine
import land.src.toolbox.remote.impl.LinuxRemoteProcess
import land.src.toolbox.remote.impl.WindowsRemoteProcess
import org.objectweb.asm.ClassReader
import java.io.DataOutputStream
import java.io.File

fun main() {
    val proc = System.getProperty("os.name").let {
        when {
            it.contains("linux", ignoreCase = true) -> LinuxRemoteProcess.current
            it.contains("windows", ignoreCase = true) -> WindowsRemoteProcess.current
            else -> error("Unsupported OS: $it")
        }
    }

    proc.attach()
    val vm = VirtualMachine(proc)

    //vm.print()
    val universe = Universe(vm)
    val stringKlass = universe.instanceKlass("java/lang/String")!!

    val fileOutput = DataOutputStream(File("String.class").outputStream())
    val klassDumper = KlassDumper(vm, stringKlass, fileOutput)

    klassDumper.writeClassFileFormat()

    ClassReader(File("String.class").readBytes())

    proc.detach()
}