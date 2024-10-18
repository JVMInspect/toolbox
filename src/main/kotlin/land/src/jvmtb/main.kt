package land.src.jvmtb

import land.src.jvmtb.jvm.*
import land.src.jvmtb.jvm.oop.ClassLoaderDataGraph
import land.src.jvmtb.jvm.oop.Klass
import land.src.jvmtb.remote.impl.WindowsRemoteProcess

fun main() {
    val proc = WindowsRemoteProcess.getJavaProcesses().first()
    val vm = VirtualMachine(proc)
    val version: VMVersion = vm.structs()
    println(version)
    val graph: ClassLoaderDataGraph = vm.structs()
    for (loadedClass in graph.getLoadedClasses()) {
        println(loadedClass.name.string)
    }
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