package land.src.toolbox.jvm

import land.src.toolbox.jvm.oop.ClassLoaderData
import land.src.toolbox.jvm.oop.ClassLoaderDataGraph
import land.src.toolbox.jvm.oop.InstanceKlass
import land.src.toolbox.jvm.oop.Klass
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