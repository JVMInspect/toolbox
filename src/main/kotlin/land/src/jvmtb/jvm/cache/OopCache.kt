package land.src.jvmtb.jvm.cache

import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.oop.Oop
import kotlin.reflect.KClass

class OopCache(private val scope: VMScope) : Factory {
    private val map = mutableMapOf<Long, Oop>()

    inline operator fun <reified O : Oop> invoke(address: Long): O =
        invoke(O::class, address) as O

    override operator fun invoke(type: KClass<*>, address: Long): Oop =
        map.computeIfAbsent(address) { scope.structs(type, address) as Oop }
}