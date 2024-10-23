package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KProperty

class OffsetImpl(struct: Struct, fieldName: String): Scope by struct {
    private val offset: Long by lazy {
        val field = structFields(struct, fieldName) ?: throw NoSuchFieldException("${struct.type.name}#$fieldName")
        require(!field.isStatic) {
            "Tried to access offset of static field ${struct.type.name}#$fieldName, use address(...) to access the address"
        }
        field.offsetOrAddress
    }

    operator fun getValue(struct: Struct, property: KProperty<*>) = offset
}

fun Struct.offset(fieldName: String) = OffsetImpl(this, fieldName)