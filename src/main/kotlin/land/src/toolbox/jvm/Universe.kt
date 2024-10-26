package land.src.toolbox.jvm

import land.src.toolbox.jvm.oop.*
import java.util.LinkedList

class Universe(val scope: Scope) {
    @OptIn(ExperimentalStdlibApi::class)
    val loadedKlasses: Map<String, Klass> by lazy {
        val result: MutableMap<String, Klass> = mutableMapOf()

        val graph: ClassLoaderDataGraph = scope.structs(-1)!!

        var cld: ClassLoaderData? = graph.head
        while (cld != null) {
            //val r = runCatching {
            //    val loaderOop = cld!!.loader?.obj
            //    println("loaderOop: ${loaderOop!!.toHexString()}")
            //    val oopDesc = scope.structs<OopDesc>(loaderOop!!)
            //    println("oopdesc: ${oopDesc!!.address.base.toHexString()}")
            //    println(oopDesc!!.klass.name.string)
            //}
            //if (r.isFailure) {
            //    r.exceptionOrNull()?.printStackTrace()
            //}

            var klass: Klass? = cld.klasses
            while (klass != null) {
                result[klass.name.string] = klass
                klass = klass.nextLink
            }
            cld = cld.next
        }

        result
    }

    fun klass(name: String): Klass? {
        return loadedKlasses[name]
    }

    fun instanceKlass(name: String): InstanceKlass? {
        return klass(name)?.instanceKlass
    }
}