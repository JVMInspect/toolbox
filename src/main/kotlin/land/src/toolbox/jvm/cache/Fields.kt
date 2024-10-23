package land.src.toolbox.jvm.cache

import land.src.toolbox.jvm.primitive.Field
import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.dsl.FieldLocation
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes

typealias StructAndFieldName = Pair<KClass<*>, String>

class Fields(scope: Scope) : Scope by scope {
    private val cache = mutableMapOf<StructAndFieldName, Field?>()
    private val offsets = mutableMapOf<StructAndFieldName, Long>()
    private val addresses = mutableMapOf<StructAndFieldName, Long>()

    private fun supers(baseType: KClass<*>): List<KClass<*>> {
        val value = mutableListOf<KClass<*>>()
        var type: Class<*> = baseType.java.superclass
        while (structs.isStruct(type) && type != Struct::class.java) {
            value += type.kotlin
            type = type.superclass
        }
        return value
    }

    fun offset(struct: Struct, name: String) =
        offsets[struct::class to name] ?: error("${struct.type.name}#$name has not been mapped to an offset!")

    fun address(struct: Struct, name: String) =
        addresses[struct::class to name] ?: error("${struct.type.name}#$name has not been mapped to an address!")

    fun putLocation(struct: Struct, name: String, location: FieldLocation<*>) {
        when (location) {
            is FieldLocation.Name -> {
                val field = this(struct, location.value) ?: error("Could not find ${struct.type.name}#${location.value}")
                val map = if (field.isStatic) addresses else offsets
                map.computeIfAbsent(struct::class to name) {
                    field.offsetOrAddress
                }
            }
            is FieldLocation.Offset -> {
                offsets.computeIfAbsent(struct::class to name) {
                    location.value
                }
            }
            is FieldLocation.Address -> {
                addresses.computeIfAbsent(struct::class to name) {
                    location.value
                }
            }
        }
    }

    operator fun invoke(struct: Struct, name: String) = cache.computeIfAbsent(struct::class to name) {
        struct.type.field(name) ?: supers(struct::class).firstNotNullOfOrNull {
            structs(Address.PLACEHOLDER, it)?.type?.field(name)
        }
    }
}