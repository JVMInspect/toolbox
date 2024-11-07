package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KProperty

class AddressImpl(struct: Struct, fieldName: String): Scope by struct {
    private val address: Long by lazy {
        val field = structFields(struct, fieldName) ?: throw NoSuchFieldException("${struct.vmType.name}#$fieldName")
        if (field.isStatic) field.offsetOrAddress else struct.address.base + field.offsetOrAddress
    }

    operator fun getValue(struct: Struct, property: KProperty<*>) = address
}

fun Struct.address(fieldName: String) = AddressImpl(this, fieldName)