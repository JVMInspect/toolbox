package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class BaseFieldDelegate<V : Any>(
    val struct: Struct,
    val type: KClass<V>,
    val fieldName: String,
    val isPointer: Boolean
) : Scope by struct {
    init {
        if (arrays.isArray(type))
            throw IllegalArgumentException("Declare arrays using nonNullArray or maybeNullArray.")
    }

    interface Factory<V> {
        operator fun invoke(): V?
    }

    class StructFactory<S : Any>(
        scope: Scope,
        private val fieldAddress: Long,
        private val structType: KClass<S>
    ) : Scope by scope, Factory<S> {
        @Suppress("Unchecked_Cast")
        override fun invoke(): S? = structs(fieldAddress, structType) as? S?
    }

    class OopFactory<O : Any>(
        scope: Scope,
        private val fieldAddress: Long,
        private val oopType: KClass<O>
    ) : Scope by scope, Factory<O> {
        @Suppress("Unchecked_Cast")
        override fun invoke(): O? = oops(fieldAddress, oopType) as? O?
    }

    protected val factory: Factory<V>? by lazy {
        if (oops.isOop(type))
            return@lazy OopFactory<V>(this, fieldAddress, type)
        if (structs.isStruct(type))
            return@lazy StructFactory<V>(this, fieldAddress, type)
        else null
    }

    protected val fieldAddress by lazy {
        val field = structFields(struct, fieldName) ?: throw NoSuchFieldException("${struct.type.name}#$fieldName")
        val base = struct.address.base
        val address = if (field.isStatic) field.offsetOrAddress else base + field.offsetOrAddress

        if (isPointer || structs.isStruct(type))
            unsafe.getAddress(address)
        else address
    }
}

open class NullableFieldDelegate<V : Any>(
    struct: Struct,
    type: KClass<V>,
    fieldName: String,
    isPointer: Boolean
) : BaseFieldDelegate<V>(struct, type, fieldName, isPointer) {
    @Suppress("Unchecked_Cast")
    open operator fun getValue(thisRef: Struct, property: KProperty<*>): V? {
        if (factory != null)
            return factory!!()

        return when (this.type) {
            Byte::class -> unsafe.getByte(fieldAddress)
            Short::class -> unsafe.getShort(fieldAddress)
            Char::class -> unsafe.getChar(fieldAddress)
            Int::class -> unsafe.getInt(fieldAddress)
            Long::class -> unsafe.getLong(fieldAddress)
            Float::class -> unsafe.getFloat(fieldAddress)
            Double::class -> unsafe.getDouble(fieldAddress)
            String::class -> unsafe.getString(fieldAddress)
            else -> error("${this.type.simpleName} getter is not supported")
        } as? V?
    }
}

class FieldDelegate<V : Any>(
    struct: Struct,
    type: KClass<V>,
    fieldName: String,
    isPointer: Boolean
) : NullableFieldDelegate<V>(struct, type, fieldName, isPointer) {
    override operator fun getValue(thisRef: Struct, property: KProperty<*>): V =
        super.getValue(thisRef, property)
            ?: throw NullPointerException("${struct.typeName}#$fieldName isPointer: $isPointer")
}

inline fun <reified V : Any> Struct.maybeNull(fieldName: String, isPointer: Boolean = false) = NullableFieldDelegate(
    type = V::class,
    struct = this,
    fieldName = fieldName,
    isPointer = isPointer
)

inline fun <reified V : Any> Struct.nonNull(fieldName: String, isPointer: Boolean = false) = FieldDelegate(
    type = V::class,
    struct = this,
    fieldName = fieldName,
    isPointer = isPointer
)