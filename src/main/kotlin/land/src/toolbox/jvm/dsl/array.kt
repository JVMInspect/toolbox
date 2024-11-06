package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class BaseArrayFieldDelegate<E : Any, A : Array<E>>(
    val struct: Struct,
    val arrayType: KClass<in A>,
    val elementType: KClass<E>,
    val isElementPointer: Boolean,
    val location: FieldLocation<*>
) : Scope by struct {
    protected val address: AddressProvider by lazy {
        when (location) {
            is FieldLocation.Name -> NamedFieldAddressProvider(struct, arrayType, location)
            is FieldLocation.Offset -> OffsetFieldAddressProvider(struct, arrayType, location)
            is FieldLocation.Address -> AddressFieldAddressProvider(struct, arrayType, location)
        }
    }
}

open class NullableArrayFieldDelegate<E : Any, A : Array<E>>(
    struct: Struct,
    arrayType: KClass<in A>,
    elementType: KClass<E>,
    isElementPointer: Boolean,
    location: FieldLocation<*>
) : BaseArrayFieldDelegate<E, A>(struct, arrayType, elementType, isElementPointer, location) {
    @Suppress("Unchecked_Cast")
    open operator fun getValue(struct: Struct, property: KProperty<*>): A? {
        structFields.put(struct, property.name, location)

        return arrays(address(), arrayType, elementType, isElementPointer) as? A?
    }

    open operator fun setValue(struct: Struct, property: KProperty<*>, value: A?) {
        unsafe.putAddress(address(), value?.address?.base ?: 0)
    }
}

class ArrayFieldDelegate<E : Any, A : Array<E>>(
    struct: Struct,
    arrayType: KClass<in A>,
    elementType: KClass<E>,
    isElementPointer: Boolean,
    location: FieldLocation<*>
) : NullableArrayFieldDelegate<E, A>(struct, arrayType, elementType, isElementPointer, location) {
    override operator fun getValue(struct: Struct, property: KProperty<*>): A =
        super.getValue(struct, property) ?: throw NullPointerException("${struct.typeName}#$location")
}

inline fun <reified E : Any, reified A : Array<E>?> Struct.maybeNullArray(
    fieldName: String,
    isElementPointer: Boolean = structs.isStruct(E::class)
) = NullableArrayFieldDelegate(
    struct = this,
    arrayType = A::class,
    elementType = E::class,
    isElementPointer = isElementPointer,
    location = FieldLocation.Name(fieldName, true)
)

inline fun <reified E : Any, reified A : Array<E>?> Struct.maybeNullArray(
    isElementPointer: Boolean = structs.isStruct(E::class),
    block: FieldLocationProviderScope.() -> FieldLocation<*>
): NullableArrayFieldDelegate<E, A & Any> {
    val provider = FieldLocationProvider(this)
    val location = block(provider)
    return NullableArrayFieldDelegate(
        struct = this,
        arrayType = A::class,
        elementType = E::class,
        isElementPointer = isElementPointer,
        location = location
    )
}

inline fun <reified E : Any, reified A : Array<E>> Struct.nonNullArray(
    fieldName: String,
    isElementPointer: Boolean = structs.isStruct(E::class)
) = ArrayFieldDelegate(
    struct = this,
    arrayType = A::class,
    elementType = E::class,
    isElementPointer = isElementPointer,
    location = FieldLocation.Name(fieldName, true)
)

inline fun <reified E : Any, reified A : Array<E>> Struct.nonNullArray(
    isElementPointer: Boolean = structs.isStruct(E::class),
    block: FieldLocationProviderScope.() -> FieldLocation<*>
) : ArrayFieldDelegate<E, A> {
    val provider = FieldLocationProvider(this)
    val location = block(provider)
    return ArrayFieldDelegate(
        struct = this,
        arrayType = A::class,
        elementType = E::class,
        isElementPointer = isElementPointer,
        location = location
    )
}