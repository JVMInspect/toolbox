package land.src.toolbox.jvm.cache

import land.src.toolbox.jvm.primitive.Field
import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes

typealias StructAndFieldName = Pair<KClass<*>, String>

class Fields(scope: Scope) : Scope by scope {
    private val cache = mutableMapOf<StructAndFieldName, Field?>()

    private fun supers(baseType: KClass<*>): List<KClass<*>> {
        val value = mutableListOf<KClass<*>>()
        var type: Class<*> = baseType.java.superclass
        while (structs.isStruct(type) && type != Struct::class.java) {
            value += type.kotlin
            type = type.superclass
        }
        return value
    }

    operator fun invoke(struct: Struct, name: String) = cache.computeIfAbsent(struct::class to name) {
        struct.type.field(name) ?: supers(struct::class).firstNotNullOfOrNull {
            structs(Address.PLACEHOLDER, it)?.type?.field(name)
        }
    }
}