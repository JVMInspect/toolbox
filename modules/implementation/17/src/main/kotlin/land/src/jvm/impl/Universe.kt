package land.src.jvm.impl

import land.src.jvm.impl.oop.ClassLoaderData
import land.src.jvm.impl.oop.ClassLoaderDataGraph
import land.src.jvm.impl.oop.Klass
import land.src.toolbox.jvm.Scope

class Universe(val scope: Scope) {
    val loadedKlasses: Map<String, Klass> get() = buildMap {
        val graph: ClassLoaderDataGraph = scope.structs(-1)!!
        var cld: ClassLoaderData? = graph.head
        while (cld != null) {
            putAll(cld.allKlasses)
            cld = cld.next
        }
    }
}