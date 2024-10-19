package land.src.jvmtb.dsl

import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.jvm.oop.Oop
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

val Struct.mappedTypeName get() =
    typeName ?: this::class.simpleName ?: error("Struct ${this::class.java.name} does not have a type name.")

class FieldImpl<V : Any>(
    private val type: KClass<V>,
    struct: Struct,
    fieldName: String,
    isOop: Boolean,
    isStruct: Boolean,
    isPointer: Boolean
) {
    private val machine = struct.address.scope.vm
    private val factory = if (isOop) machine.oops else if (isStruct) machine.structs else null

    private val fieldAddress: Long by lazy {
        val base = struct.address.base
        val type = machine.type(struct.mappedTypeName)
        val field = type.field(fieldName)
        val address = if (field.isStatic) field.offsetOrAddress else base + field.offsetOrAddress
        if (!isPointer) address else machine.getAddress(address)
    }

    operator fun setValue(thisRef: Struct, property: KProperty<*>, value: V) {
        return factory?.invoke(this.type, machine.getAddress(fieldAddress)) as? Unit ?: when (this.type) {
            Byte::class -> machine.putByte(fieldAddress, value as Byte)
            Short::class -> machine.putShort(fieldAddress, value as Short)
            Char::class -> machine.putChar(fieldAddress, value as Char)
            Int::class -> machine.putInt(fieldAddress, value as Int)
            Long::class -> machine.putLong(fieldAddress, value as Long)
            Float::class -> machine.putFloat(fieldAddress, value as Float)
            Double::class -> machine.putDouble(fieldAddress, value as Double)
            // todo: check sizing
            String::class -> machine.putMemory(fieldAddress, (value as String).toByteArray())
            else -> error("${this.type.simpleName} setter is not supported")
        }
    }

    @Suppress("Unchecked_Cast")
    operator fun getValue(thisRef: Struct, property: KProperty<*>): V {
        return factory?.invoke(this.type, machine.getAddress(fieldAddress)) as? V ?: when (this.type) {
            Byte::class -> machine.getByte(fieldAddress)
            Short::class -> machine.getShort(fieldAddress)
            Char::class -> machine.getChar(fieldAddress)
            Int::class -> machine.getInt(fieldAddress)
            Long::class -> machine.getLong(fieldAddress)
            Float::class -> machine.getFloat(fieldAddress)
            Double::class -> machine.getDouble(fieldAddress)
            String::class -> machine.getString(fieldAddress)
            else -> error("${this.type.simpleName} getter is not supported")
        } as V
    }
}

class OffsetImpl(struct: Struct, fieldName: String) {
    private val offset: Long by lazy {
        val machine = struct.address.scope.vm
        val type = machine.type(struct.mappedTypeName)
        val field = type.field(fieldName)
        field.offsetOrAddress
    }

    operator fun getValue(thisRef: Struct, property: KProperty<*>) = offset
}

class ArrayImpl<E : Any, A : Array<E>>(
    struct: Struct,
    fieldName: String,
    val arrayType: KClass<A>,
    val elementType: KClass<E>,
    isPointer: Boolean
) {
    private val machine = struct.address.scope.vm

    private val fieldAddress: Long by lazy {
        val base = struct.address.base
        val type = machine.type(struct.mappedTypeName)
        val field = type.field(fieldName)
        val address = if (field.isStatic) field.offsetOrAddress else base + field.offsetOrAddress
        if (!isPointer) address else machine.getAddress(address)
    }

    operator fun getValue(thisRef: Struct, property: KProperty<*>): A {
        return machine.arrays(elementType, arrayType, fieldAddress)
    }
}

inline fun <reified E : Any, reified A : Array<E>> Struct.array(fieldName: String) = ArrayImpl(
    struct = this,
    fieldName = fieldName,
    arrayType = A::class,
    elementType = E::class,
    isPointer = false
)

fun Struct.offset(fieldName: String) = OffsetImpl(
    struct = this,
    fieldName = fieldName
)

fun Struct.int(fieldName: String, isPointer: Boolean = false) = FieldImpl(
    type = Int::class,
    struct = this,
    fieldName = fieldName,
    isOop = false,
    isStruct = false,
    isPointer = isPointer
)

fun Struct.short(fieldName: String, isPointer: Boolean = false) = FieldImpl(
    type = Short::class,
    struct = this,
    fieldName = fieldName,
    isOop = false,
    isStruct = false,
    isPointer = isPointer
)

fun Struct.long(fieldName: String, isPointer: Boolean = false) = FieldImpl(
    type = Long::class,
    struct = this,
    fieldName = fieldName,
    isOop = false,
    isStruct = false,
    isPointer = isPointer
)

fun Struct.string(fieldName: String, isPointer: Boolean = false) = FieldImpl(
    type = String::class,
    struct = this,
    fieldName = fieldName,
    isOop = false,
    isStruct = false,
    isPointer = isPointer
)

inline fun <reified O : Oop> Struct.oop(fieldName: String, isPointer: Boolean = false) = FieldImpl(
    type = O::class,
    struct = this,
    fieldName = fieldName,
    isOop = true,
    isStruct = false,
    isPointer = isPointer
)

inline fun <reified S : Struct> Struct.struct(fieldName: String, isPointer: Boolean = false) = FieldImpl(
    type = S::class,
    struct = this,
    fieldName = fieldName,
    isOop = false,
    isStruct = true,
    isPointer = isPointer
)