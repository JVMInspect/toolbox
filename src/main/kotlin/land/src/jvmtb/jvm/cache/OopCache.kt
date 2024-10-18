package land.src.jvmtb.jvm.cache

import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.oop.Oop
import kotlin.reflect.KClass

class OopCache(val scope: VMScope) : Factory {
    val map = mutableMapOf<Long, Oop>()

    inline operator fun <reified O : Oop> invoke(address: Long): O
        = map.computeIfAbsent(address) { scope.structs<O>(address) } as O

    override operator fun invoke(type: KClass<*>, address: Long): Any
            = map.computeIfAbsent(address) { scope.structs(type, address) as Oop }
}