package land.src.jvmtb

import land.src.jvmtb.jvm.*
import land.src.jvmtb.jvm.oop.*
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.remote.impl.LinuxRemoteProcess
import land.src.jvmtb.remote.impl.WindowsRemoteProcess

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
    //val graph: ClassLoaderDataGraph = vm.structs()
    //for (loadedClass in graph.getLoadedClasses()) {
    //    println(loadedClass.name.string)
    //    val supers = loadedClass.secondarySupers
    //    if (supers != null) {
    //        for (s in supers) {
    //            println("\tsuper: "+ s.name.string)
    //        }
    //    }
    //}
    proc.detach()
}

fun ClassLoaderDataGraph.getLoadedClasses(): List<Klass> {
    val result: MutableList<Klass> = ArrayList()

    var cld: ClassLoaderData? = head
    while (cld != null) {
        var klass: Klass? = cld.klasses
        while (klass != null) {
            result.add(klass)
            klass = klass.nextLink
        }
        cld = cld.next
    }

    return result
}