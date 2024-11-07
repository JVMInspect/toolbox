package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Address
import land.src.jvm.api.Addressable
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

sealed class FieldLocation<V>(val value: V, val isPointer: Boolean) {
    class Name(value: String, isPointer: Boolean) : FieldLocation<String>(value, isPointer)
    class Offset(value: Long, isPointer: Boolean) : FieldLocation<Long>(value, isPointer)
    class Address(value: Long, isPointer: Boolean) : FieldLocation<Long>(value, isPointer)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = when (this) {
        is Name -> value
        is Offset -> "+0x${value.toHexString()}"
        is Address -> "0x${value.toHexString()}"
    }
}

interface AddressProvider {
    operator fun invoke(usePointer: Boolean = true): Long
}

class NamedFieldAddressProvider<V : Any>(
    val struct: Struct,
    val type: KClass<V>,
    val location: FieldLocation.Name
) : AddressProvider, Scope by struct {
    override fun invoke(usePointer: Boolean): Long {
        val name = location.value
        val field = structFields(struct, name) ?: throw NoSuchFieldException("${struct.vmType.name}#$name")
        val base = struct.address.base
        val address = if (field.isStatic) field.offsetOrAddress else base + field.offsetOrAddress

        return if (usePointer && (location.isPointer || structs.isStruct(type)))
            unsafe.getAddress(address)
        else address
    }
}

class OffsetFieldAddressProvider<V : Any>(
    val struct: Struct,
    val type: KClass<V>,
    val location: FieldLocation.Offset
) : AddressProvider, Scope by struct {
    override fun invoke(usePointer: Boolean): Long {
        val base = struct.address.base
        val address = base + location.value

        return if (usePointer && (location.isPointer || structs.isStruct(type)))
            unsafe.getAddress(address)
        else address
    }
}

class AddressFieldAddressProvider<V : Any>(
    val struct: Struct,
    val type: KClass<V>,
    val location: FieldLocation.Address
) : AddressProvider, Scope by struct {
    override fun invoke(usePointer: Boolean): Long {
        val address = location.value
        return if (usePointer && (location.isPointer || structs.isStruct(type)))
            unsafe.getAddress(address)
        else address
    }
}

abstract class BaseFieldDelegate<V : Any>(
    val struct: Struct,
    val type: KClass<V>,
    val location: FieldLocation<*>
) : Scope by struct {
    interface Factory<V> {
        operator fun invoke(): V?
    }

    class StructFactory<S : Any>(
        scope: Scope,
        private val address: AddressProvider,
        private val structType: KClass<S>
    ) : Scope by scope, Factory<S> {
        @Suppress("Unchecked_Cast")
        override fun invoke(): S? = structs(address(), structType) as? S?
    }

    class OopFactory<O : Any>(
        scope: Scope,
        private val address: AddressProvider,
        private val oopType: KClass<O>
    ) : Scope by scope, Factory<O> {
        @Suppress("Unchecked_Cast")
        override fun invoke(): O? = oops(address(), oopType) as? O?
    }

    protected val factory: Factory<V>? by lazy {
        if (oops.isOop(type))
            return@lazy OopFactory<V>(this, address, type)
        if (structs.isStruct(type))
            return@lazy StructFactory<V>(this, address, type)
        else null
    }

    protected val address: AddressProvider by lazy {
        when (location) {
            is FieldLocation.Name -> NamedFieldAddressProvider(struct, type, location)
            is FieldLocation.Offset -> OffsetFieldAddressProvider(struct, type, location)
            is FieldLocation.Address -> AddressFieldAddressProvider(struct, type, location)
        }
    }
}

open class NullableFieldDelegate<V : Any>(
    struct: Struct,
    type: KClass<V>,
    location: FieldLocation<*>
) : BaseFieldDelegate<V>(struct, type, location) {
    @Suppress("Unchecked_Cast")
    open operator fun getValue(struct: Struct, property: KProperty<*>): V? {
        structFields.put(struct, property.name, location)

        if (factory != null)
            return factory!!()

        val address = address()

        if (type == Address::class)
            return Address(struct, unsafe.getAddress(address)) as V?

        return when (type) {
            Byte::class -> unsafe.getByte(address)
            Short::class -> unsafe.getShort(address)
            Char::class -> unsafe.getChar(address)
            Int::class -> unsafe.getInt(address)
            Long::class -> unsafe.getLong(address)
            Float::class -> unsafe.getFloat(address)
            Double::class -> unsafe.getDouble(address)
            String::class -> unsafe.getString(address)
            else -> error("${type.simpleName} getter is not supported")
        } as? V?
    }

    operator fun setValue(struct: Struct, property: KProperty<*>, value: V?) {
        structFields.put(struct, property.name, location)

        if (globals.isAddressable(type))
            return unsafe.putAddress(address(false), (value as? Addressable)?.base ?: 0)

        val address = address()

        when (type) {
            Byte::class -> unsafe.putByte(address, value as? Byte ?: 0)
            Short::class -> unsafe.putShort(address, value as? Short ?: 0)
            Char::class -> unsafe.putChar(address, value as? Char ?: 0.toChar())
            Int::class -> unsafe.putInt(address, value as? Int ?: 0)
            Long::class -> unsafe.putLong(address, value as? Long ?: 0)
            Float::class -> unsafe.putFloat(address, value as? Float ?: 0f)
            Double::class -> unsafe.putDouble(address, value as? Double ?: 0.0)
            else -> error("${type.simpleName} setter is not supported")
        }
    }
}

class FieldDelegate<V : Any>(
    struct: Struct,
    type: KClass<V>,
    location: FieldLocation<*>,
) : NullableFieldDelegate<V>(struct, type, location) {
    override operator fun getValue(struct: Struct, property: KProperty<*>): V =
        super.getValue(struct, property)
            ?: throw NullPointerException("${struct.vmType.name}#$location")
}

fun Struct.offset(prop: KProperty0<*>) =
    structFields.offset(this, prop.name)

fun Struct.address(prop: KProperty0<*>) =
    structFields.address(this, prop.name)

interface FieldLocationProviderScope {
    val major: Int
    val minor: Int
    val build: Int
    val release: String

    fun name(value: String, isPointer: Boolean = false) =
        FieldLocation.Name(value, isPointer)

    fun offset(value: Long, isPointer: Boolean = false) =
        FieldLocation.Offset(value, isPointer)

    fun address(value: Long, isPointer: Boolean = false) =
        FieldLocation.Address(value, isPointer)
}

class FieldLocationProvider(val scope: Scope) : FieldLocationProviderScope {
    override val major: Int get() = scope.version.major
    override val minor: Int get() = scope.version.minor
    override val build: Int get() = scope.version.build
    override val release: String get() = scope.version.release
}

/**
 * Internal helper function for computing the offset of a field using a [constant].
 */
inline fun <reified V : Any> Struct.constantOffsetValue(constant: String, isIndex: Boolean): Long {
    val isFullPath = constant.contains("::")

    val value =
        if (isFullPath) vm.constant(constant) as Long
        else vm.constant("${vmTypeName}::$constant") as Long

    val multiplier = if (isIndex) structs.sizeof(V::class) else 1
    return value * multiplier
}

/**
 * Creates a nullable field, which computes the field's address using the provided [fieldName].
 *
 * The field's full path will be computed as a concatenation of [Struct.vmTypeName] and [fieldName].
 *
 * @param fieldName the name of the field
 * @param isPointer if the field type is a pointer
 */
inline fun <reified V : Any> Struct.maybeNull(fieldName: String, isPointer: Boolean = false) = NullableFieldDelegate(
    type = V::class,
    struct = this,
    location = FieldLocation.Name(fieldName, isPointer)
)

/**
 * Creates a nullable field, which computes the address of the field using a [FieldLocationProviderScope].
 *
 * This scope allows the field to be initialized using a different address/offset/name depending on the current VM state,
 * usually depending on the VM version.
 *
 * @param block the field location provider which returns the location of the field when computed.
 */
inline fun <reified V : Any> Struct.maybeNull(
    block: FieldLocationProviderScope.() -> FieldLocation<*>
): NullableFieldDelegate<V> {
    val provider = FieldLocationProvider(this)
    val location = block(provider)
    return NullableFieldDelegate(type = V::class, struct = this, location = location)
}

/**
 * Creates a nullable field, which treats the constant value as an index, which will compute the field address as
 * `struct + (value * size)` where `size` is the size of [V].
 *
 * The constant's parent type name can be omitted if it resides in the same [Struct].
 *
 * @param constant the name of the constant
 * @param isPointer if the field type is a pointer
 */
inline fun <reified V : Any> Struct.maybeNullFromConstantIndex(
    constant: String,
    isPointer: Boolean = false
) = NullableFieldDelegate(
    type = V::class,
    struct = this,
    location = FieldLocation.Offset(constantOffsetValue<V>(constant, true), isPointer)
)

/**
 * Creates a nullable field, which treats the constant value as an offset, which will compute the field address as
 * `struct + value` where `value` is the constant value.
 *
 * The constant's parent type name can be omitted if it resides in the same [Struct].
 *
 * @param constant the name of the constant
 * @param isPointer if the field type is a pointer
 */
inline fun <reified V : Any> Struct.maybeNullFromConstantOffset(
    constant: String,
    isPointer: Boolean = false
) = NullableFieldDelegate(
    type = V::class,
    struct = this,
    location = FieldLocation.Offset(constantOffsetValue<V>(constant, false), isPointer)
)

/**
 * Creates a field, which computes the field's address using the provided [fieldName].
 *
 * The field's full path will be computed as a concatenation of [Struct.vmTypeName] and [fieldName].
 *
 * @param fieldName the name of the field
 * @param isPointer if the field type is a pointer
 */
inline fun <reified V : Any> Struct.nonNull(fieldName: String, isPointer: Boolean = false) = FieldDelegate(
    type = V::class,
    struct = this,
    location = FieldLocation.Name(fieldName, isPointer)
)

/**
 * Creates a field, which computes the address of the field using a [FieldLocationProviderScope].
 *
 * This scope allows the field to be initialized using a different address/offset/name depending on the current VM state,
 * usually depending on the VM version.
 *
 * @param block the field location provider which returns the location of the field when computed.
 */
inline fun <reified V : Any> Struct.nonNull(
    block: FieldLocationProviderScope.() -> FieldLocation<*>
): FieldDelegate<V> {
    val provider = FieldLocationProvider(this)
    val location = block(provider)
    return FieldDelegate(type = V::class, struct = this, location = location)
}

/**
 * Creates a field, which treats the constant value as an index, which will compute the field address as
 * `struct + (value * size)` where `size` is the size of [V].
 *
 * The constant's parent type name can be omitted if it resides in the same [Struct].
 *
 * @param constant the name of the constant
 * @param isPointer if the field type is a pointer
 */
inline fun <reified V : Any> Struct.nonNullFromConstantIndex(
    constant: String,
    isPointer: Boolean = false
) = FieldDelegate(
    type = V::class,
    struct = this,
    location = FieldLocation.Offset(constantOffsetValue<V>(constant, true), isPointer)
)

/**
 * Creates a field, which treats the constant value as an offset, which will compute the field address as
 * `struct + value` where `value` is the constant value.
 *
 * The constant's parent type name can be omitted if it resides in the same [Struct].
 *
 * @param constant the name of the constant
 * @param isPointer if the field type is a pointer
 */
inline fun <reified V : Any> Struct.nonNullFromConstantOffset(
    constant: String,
    isPointer: Boolean = false
) = FieldDelegate(
    type = V::class,
    struct = this,
    location = FieldLocation.Offset(constantOffsetValue<V>(constant, false), isPointer)
)