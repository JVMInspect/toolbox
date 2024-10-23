package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.VMVersion
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
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
    operator fun invoke(): Long
}

class NamedFieldAddressProvider<V : Any>(
    val struct: Struct,
    val type: KClass<V>,
    val location: FieldLocation.Name
) : AddressProvider, Scope by struct {
    override fun invoke(): Long {
        val name = location.value
        val field = structFields(struct, name) ?: throw NoSuchFieldException("${struct.type.name}#$name")
        val base = struct.address.base
        val address = if (field.isStatic) field.offsetOrAddress else base + field.offsetOrAddress

        return if (location.isPointer || structs.isStruct(type))
            unsafe.getAddress(address)
        else address
    }
}

class OffsetFieldAddressProvider<V : Any>(
    val struct: Struct,
    val type: KClass<V>,
    val location: FieldLocation.Offset
) : AddressProvider, Scope by struct {
    override fun invoke(): Long {
        val base = struct.address.base
        val address = base + location.value

        return if (location.isPointer || structs.isStruct(type))
            unsafe.getAddress(address)
        else address
    }
}

class AddressFieldAddressProvider<V : Any>(
    val struct: Struct,
    val type: KClass<V>,
    val location: FieldLocation.Address
) : AddressProvider, Scope by struct {
    override fun invoke(): Long {
        val address = location.value
        return if (location.isPointer || structs.isStruct(type))
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
        structFields.putLocation(struct, property.name, location)

        if (factory != null)
            return factory!!()

        return when (this.type) {
            Byte::class -> unsafe.getByte(address())
            Short::class -> unsafe.getShort(address())
            Char::class -> unsafe.getChar(address())
            Int::class -> unsafe.getInt(address())
            Long::class -> unsafe.getLong(address())
            Float::class -> unsafe.getFloat(address())
            Double::class -> unsafe.getDouble(address())
            String::class -> unsafe.getString(address())
            else -> error("${this.type.simpleName} getter is not supported")
        } as? V?
    }
}

class FieldDelegate<V : Any>(
    struct: Struct,
    type: KClass<V>,
    location: FieldLocation<*>,
) : NullableFieldDelegate<V>(struct, type, location) {
    override operator fun getValue(struct: Struct, property: KProperty<*>): V =
        super.getValue(struct, property)
            ?: throw NullPointerException("${struct.typeName}#$location")
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

inline fun <reified V : Any> Struct.maybeNull(fieldName: String, isPointer: Boolean = false) = NullableFieldDelegate(
    type = V::class,
    struct = this,
    location = FieldLocation.Name(fieldName, isPointer)
)

inline fun <reified V : Any> Struct.maybeNull(
    block: FieldLocationProviderScope.() -> FieldLocation<*>
): NullableFieldDelegate<V> {
    val provider = FieldLocationProvider(this)
    val location = block(provider)
    return NullableFieldDelegate(type = V::class, struct = this, location = location)
}

inline fun <reified V : Any> Struct.nonNull(fieldName: String, isPointer: Boolean = false) = FieldDelegate(
    type = V::class,
    struct = this,
    location = FieldLocation.Name(fieldName, isPointer)
)

inline fun <reified V : Any> Struct.nonNull(
    block: FieldLocationProviderScope.() -> FieldLocation<*>
): FieldDelegate<V> {
    val provider = FieldLocationProvider(this)
    val location = block(provider)
    return FieldDelegate(type = V::class, struct = this, location = location)
}