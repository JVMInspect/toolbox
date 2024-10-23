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
        val currentType = cache[address]?.let { it::class } ?: oopType
        if (currentType != oopType) {
            require(currentType.java.isAssignableFrom(oopType.java)) {
                "Replacement oop type must be assignable from the current type"
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