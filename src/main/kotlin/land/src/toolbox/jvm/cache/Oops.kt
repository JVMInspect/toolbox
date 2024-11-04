package land.src.toolbox.jvm.cache

import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.Scope
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class Oops(scope: Scope) : Scope by scope {
    private val cache = mutableMapOf<Long, Oop?>()

    fun isOop(type: KClass<*>): Boolean =
        Oop::class.java.isAssignableFrom(type.java)

    operator fun invoke(address: Long, oopType: KClass<*>): Oop? {
        if (address == 0L)
            return null

        val currentType = cache[address]?.let { it::class } ?: oopType
        if (currentType != oopType) {
            val baseAssignable = currentType.java.isAssignableFrom(oopType.java)
            val superAssignable = currentType.java.superclass.isAssignableFrom(oopType.java)
            // todo: be more lenient?
            require(baseAssignable || superAssignable) {
                "Replacement oop type ($oopType) must be assignable from the current type ($currentType)"
            }
            val oop = structs(address, oopType) as? Oop?
            cache[address] = oop
            return oop
        }

        return cache.computeIfAbsent(address) {
            structs(address, oopType) as? Oop?
        }
    }

    inline operator fun <reified O : Oop> invoke(address: Long): O? =
        this(address, O::class) as? O?
}