package land.src.jvmtb

import land.src.jvmtb.jvm.*
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.jvm.oop.ClassLoaderDataGraph
import land.src.jvmtb.jvm.oop.Klass
import land.src.jvmtb.jvm.oop.Symbol
import land.src.jvmtb.remote.impl.LinuxRemoteProcess
import land.src.jvmtb.remote.impl.WindowsRemoteProcess

fun main() {
    val remotes = System.getProperty("os.name").let {
        when {
            it.contains("windows", ignoreCase = true) -> WindowsRemoteProcess.remotes
            it.contains("linux", ignoreCase = true) -> LinuxRemoteProcess.remotes
            else -> error("Unsupported OS: $it")
        }
    }

    val proc = remotes.first()
    proc.attach()
    val vm = VirtualMachine(proc)
    val version: VMVersion = vm.structs()
    println(version)
    val graph: ClassLoaderDataGraph = vm.structs()
    for (loadedClass in graph.getLoadedClasses()) {
        for (i in loadedClass.secondarySupers) {
            println(i.name)
        }

        println(loadedClass.name.string)
        println(loadedClass.structs.sizeOf<Symbol>())
    }
    proc.detach()
}

fun ClassLoaderDataGraph.getLoadedClasses(): List<Klass> {
    val result: MutableList<Klass> = ArrayList()

    var cld = head
    while (cld.address.base != 0L) {
        var klass = cld.klasses
        while (klass.address.base != 0L) {
            result.add(klass)
            klass = klass.nextLink
        }
        cld = cld.next
    }

    return result
}