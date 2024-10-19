package land.src.jvmtb.dsl

import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.util.isStruct
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class BaseArrayField<E : Any, A : Array<E>?>(
    struct: Struct,
    fieldName: String,
    val arrayType: KClass<*>,
    val elementType: KClass<*>,
    isPointer: Boolean,
    val isElementPointer: Boolean
) {
    protected val machine = struct.address.scope.vm
    protected val fieldAddress: Long by lazy {
        val base = struct.address.base
        val type = machine.type(struct.mappedTypeName)
        val field = type.field(fieldName)
        val address = if (field.isStatic) field.offsetOrAddress else base + field.offsetOrAddress
        if (!isPointer) address else machine.getAddress(address)
    }
}

open class NullableArrayField<E : Any, A : Array<E>?>(
    struct: Struct,
    fieldName: String,
    arrayType: KClass<*>,
    elementType: KClass<*>,
    isPointer: Boolean,
    isElementPointer: Boolean
) : BaseArrayField<E, A>(struct, fieldName, arrayType, elementType, isPointer, isElementPointer) {
    @Suppress("Unchecked_Cast")
    open operator fun getValue(thisRef: Struct, property: KProperty<*>): A? {
        if (fieldAddress == 0L) {
            return null
        }
        return machine.arrays(elementType, arrayType, isElementPointer, fieldAddress) as? A
    }
}

class ArrayField<E : Any, A : Array<E>>(
    struct: Struct,
    fieldName: String,
    arrayType: KClass<*>,
    elementType: KClass<*>,
    isPointer: Boolean,
    isElementPointer: Boolean
) : NullableArrayField<E, A>(struct, fieldName, arrayType, elementType, isPointer, isElementPointer) {
    override operator fun getValue(thisRef: Struct, property: KProperty<*>): A =
        super.getValue(thisRef, property) ?: error("Null pointer")
}

inline fun <reified E : Any, reified A : Array<E>> Struct.array(
    fieldName: String,
    isPointer: Boolean = false,
    isElementPointer: Boolean = A::class.isStruct
) = ArrayField<E, A>(
    struct = this,
    fieldName = fieldName,
    arrayType = A::class,
    elementType = E::class,
    isPointer = isPointer,
    isElementPointer = isElementPointer
)

inline fun <reified E : Any, reified A : Array<E>?> Struct.nullableArray(
    fieldName: String,
    isPointer: Boolean = false,
    isElementPointer: Boolean = A::class.isStruct
) = NullableArrayField<E, A>(
    struct = this,
    fieldName = fieldName,
    arrayType = A::class,
    elementType = E::class,
    isPointer = isPointer,
    isElementPointer = isElementPointer
)