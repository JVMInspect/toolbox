package land.src.jvmtb.jvm

import land.src.jvmtb.jvm.oop.ClassLoaderData
import land.src.jvmtb.jvm.oop.ClassLoaderDataGraph
import land.src.jvmtb.jvm.oop.InstanceKlass
import land.src.jvmtb.jvm.oop.Klass
import land.src.toolbox.jvm.Scope
import java.util.LinkedList

class Universe(val scope: Scope) {

    val loadedKlasses: LinkedList<Klass> by lazy {
        val result: LinkedList<Klass> = LinkedList()

        val graph: ClassLoaderDataGraph = scope.structs(-1)!!

        var cld: ClassLoaderData? = graph.head
        while (cld != null) {
            var klass: Klass? = cld.klasses
            while (klass != null) {
                result.add(klass)
                klass = klass.nextLink
            }
            cld = cld.next
        }

        result
    }

    fun klass(name: String): Klass? {
        return loadedKlasses.find { it.name.string == name }
    }

    fun instanceKlass(name: String): InstanceKlass? {
        return klass(name)?.instanceKlass
    }
}