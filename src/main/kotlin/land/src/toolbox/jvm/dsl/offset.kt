package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KProperty

class OffsetImpl(struct: Struct, fieldName: String): Scope by struct {
    private val offset: Long by lazy {
        val field = structFields(struct, fieldName)
        field?.offsetOrAddress ?: throw NoSuchFieldException("${struct.type.name}#$fieldName")
    }

    operator fun getValue(struct: Struct, property: KProperty<*>) = offset
}

fun Struct.offset(fieldName: String) = OffsetImpl(this, fieldName)