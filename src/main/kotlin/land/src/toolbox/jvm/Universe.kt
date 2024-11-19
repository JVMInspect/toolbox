package land.src.toolbox.jvm

import land.src.toolbox.jvm.oop.*
import land.src.toolbox.jvm.oop.gc.epsilon.EpsilonHeap
import land.src.toolbox.jvm.oop.gc.g1.G1CollectedHeap
import land.src.toolbox.jvm.oop.gc.shared.CollectedHeap
import land.src.toolbox.jvm.primitive.Address
import java.util.LinkedList

class Universe(val scope: Scope) {

    val collectedHeap: CollectedHeap? by lazy {
        val universeType = scope.vm.type("Universe")
        val heapField = universeType.field("_collectedHeap")!!
        val heapAddress = Address(scope, scope.unsafe.getAddress(heapField.offsetOrAddress))
        scope.dynamicTypeResolver.constructPolymorphic<CollectedHeap>(
            heapAddress, G1CollectedHeap::class, EpsilonHeap::class)
    }

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

            result.putAll(cld.allKlasses)
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

    fun findAllClassloadersContaining(name: String): List<ClassLoaderData> {
        val result = LinkedList<ClassLoaderData>()

        val graph: ClassLoaderDataGraph = scope.structs(-1)!!

        var cld: ClassLoaderData? = graph.head
        while (cld != null) {
            if (cld.allKlasses.containsKey(name)) {
                result.add(cld)
            }
            cld = cld.next
        }

        return result
    }
}