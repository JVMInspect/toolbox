package land.src.jvmtb.dsl

import land.src.jvmtb.jvm.Field
import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.util.isArray
import land.src.jvmtb.util.isStruct
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private val Fields = mutableMapOf<String, Field>()

abstract class BaseArrayField<E : Any>(
    struct: Struct,
    fieldName: String,
    val arrayType: KClass<out Array<E>>,
    val elementType: KClass<E>,
    isPointer: Boolean,
    val isElementPointer: Boolean
) {
    protected val machine = struct.address.scope.vm
    protected val fieldAddress: Long by lazy {
        val key = "${struct::class.simpleName}::$fieldName"
        val field = if (Fields.containsKey(key)) {
            Fields[key]!!
        } else {
            val type = struct.vm.type(struct.mappedTypeName)
            val field = type.field(fieldName)
            Fields[key] = field
            field
        }
        val base = struct.address.base
        val address = if (field.isStatic) field.offsetOrAddress else base + field.offsetOrAddress
        if (!isPointer) address else machine.getAddress(address)
    }
}

open class NullableArrayField<E : Any>(
    struct: Struct,
    fieldName: String,
    arrayType: KClass<out Array<E>>,
    elementType: KClass<E>,
    isPointer: Boolean,
    isElementPointer: Boolean
) : BaseArrayField<E>(struct, fieldName, arrayType, elementType, isPointer, isElementPointer) {
    open operator fun getValue(thisRef: Struct, property: KProperty<*>): Array<E>? {
        if (fieldAddress == 0L) {
            return null
        }
        return machine.arrays(elementType, arrayType, isElementPointer, fieldAddress)
    }
}

class ArrayField<E : Any>(
    struct: Struct,
    fieldName: String,
    arrayType: KClass<out Array<E>>,
    elementType: KClass<E>,
    isPointer: Boolean,
    isElementPointer: Boolean
) : NullableArrayField<E>(struct, fieldName, arrayType, elementType, isPointer, isElementPointer) {
    override operator fun getValue(thisRef: Struct, property: KProperty<*>): Array<E> =
        super.getValue(thisRef, property) ?: error("Null pointer")
}

inline fun <reified E : Any, reified A : Array<E>> Struct.array(
    fieldName: String,
    isPointer: Boolean = true,
    isElementPointer: Boolean = E::class.isStruct && !E::class.isArray
) = ArrayField(
    struct = this,
    fieldName = fieldName,
    arrayType = A::class,
    elementType = E::class,
    isPointer = isPointer,
    isElementPointer = isElementPointer
)

inline fun <reified E : Any, reified A : Array<E>> Struct.nullableArray(
    fieldName: String,
    isPointer: Boolean = true,
    isElementPointer: Boolean = E::class.isStruct && !E::class.isArray
) = NullableArrayField(
    struct = this,
    fieldName = fieldName,
    arrayType = A::class,
    elementType = E::class,
    isPointer = isPointer,
    isElementPointer = isElementPointer
)