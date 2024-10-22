package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class BaseArrayFieldDelegate<E : Any>(
    val struct: Struct,
    val arrayType: KClass<out Array<E>>,
    val elementType: KClass<E>,
    val isElementPointer: Boolean,
    val fieldName: String,
) : Scope by struct {
    protected val fieldAddress by lazy {
        val field = structFields(struct, fieldName) ?: throw NoSuchFieldException("${struct.type.name}#$fieldName")
        val base = struct.address.base
        val address = if (field.isStatic) field.offsetOrAddress else base + field.offsetOrAddress

        unsafe.getAddress(address)
    }
}

open class NullableArrayFieldDelegate<E : Any>(
    struct: Struct,
    arrayType: KClass<out Array<E>>,
    elementType: KClass<E>,
    isElementPointer: Boolean,
    fieldName: String,
) : BaseArrayFieldDelegate<E>(struct, arrayType, elementType, isElementPointer, fieldName) {
    @Suppress("Unchecked_Cast")
    open operator fun getValue(struct: Struct, property: KProperty<*>): Array<E>? {
        return arrays(fieldAddress, arrayType, elementType, isElementPointer) as? Array<E>?
    }
}

class ArrayFieldDelegate<E : Any>(
    struct: Struct,
    arrayType: KClass<out Array<E>>,
    elementType: KClass<E>,
    isElementPointer: Boolean,
    fieldName: String
) : NullableArrayFieldDelegate<E>(struct, arrayType, elementType, isElementPointer, fieldName) {
    override operator fun getValue(struct: Struct, property: KProperty<*>): Array<E> =
        super.getValue(struct, property) ?: throw NullPointerException("${struct.typeName}#$fieldName")
}

inline fun <reified E : Any, reified A : Array<E>> Struct.maybeNullArray(
    fieldName: String,
    isElementPointer: Boolean = structs.isStruct(E::class)
) = NullableArrayFieldDelegate(
    struct = this,
    arrayType = A::class,
    elementType = E::class,
    isElementPointer = isElementPointer,
    fieldName = fieldName,
)

inline fun <reified E : Any, reified A : Array<E>> Struct.nonNullArray(
    fieldName: String,
    isElementPointer: Boolean = structs.isStruct(E::class)
) = ArrayFieldDelegate(
    struct = this,
    arrayType = A::class,
    elementType = E::class,
    isElementPointer = isElementPointer,
    fieldName = fieldName,
)