package land.src.toolbox.jvm.primitive

import land.src.jvm.api.Addressable
import land.src.toolbox.jvm.Scope
import kotlin.reflect.KClass
import land.src.jvm.api.NArray as NArrayApi
import land.src.jvm.api.NPrimitiveArray as NPrimitiveArrayApi

open class NArray<E : Any>(address: Address, private val elementInfo: ElementInfo) : Struct(address), Oop, NArrayApi<E> {
    data class ElementInfo(
        val type: KClass<*>,
        val size: Int,
        val offset: Int,
        val isArray: Boolean,
        val isPointer: Boolean
    )

    interface Factory<V> {
        operator fun invoke(address: Long): V?
    }

    class ArrayFactory<V : Any>(
        scope: Scope,
        private val arrayType: KClass<*>,
        private val elementType: KClass<*>,
        private val isElementPointer: Boolean
    ) : Scope by scope, Factory<V> {
        @Suppress("Unchecked_Cast")
        override fun invoke(address: Long): V? = arrays(address, arrayType, elementType, isElementPointer) as? V?
    }

    class StructFactory<V : Any>(
        scope: Scope,
        private val structType: KClass<*>
    ) : Scope by scope, Factory<V> {
        @Suppress("Unchecked_Cast")
        override fun invoke(address: Long): V? = structs(address, structType) as? V?
    }

    val elementSize: Int = elementInfo.size
    val elementBase: Long = address.base + elementInfo.offset

    override val length by lazy {
        unsafe.getInt(address.base)
    }

    override val bytes by lazy {
        unsafe.getMemory(elementBase, length * elementSize)
    }

    fun getAddress(index: Int) =
        unsafe.getAddress(elementBase + index.toLong() * elementSize)

    private val factory: Factory<E>? by lazy {
        if (elementInfo.isArray) {
            val arrayType = elementInfo.type
            val elementType = arrays.getElementType(arrayType)
            val isElementPointer = elementInfo.isPointer
            ArrayFactory(this, arrayType, elementType, isElementPointer)
        }
        else if (structs.isStruct(elementInfo.type))
            StructFactory(this, elementInfo.type)
        else null
    }

    operator fun set(index: Int, value: E?) {
        if (index < 0 || index > length)
            throw IndexOutOfBoundsException(index)

        val elementAddress = elementBase + (index * elementSize).toLong()
        if (globals.isAddressable(elementInfo.type) || elementInfo.isArray) {
            return unsafe.putAddress(elementAddress, (value as? Addressable)?.base ?: 0)
        }

        when (elementInfo.type) {
            Byte::class -> unsafe.putByte(elementAddress, value as? Byte ?: 0)
            Short::class -> unsafe.putShort(elementAddress, value as? Short ?: 0)
            Char::class -> unsafe.putChar(elementAddress, value as? Char ?: 0.toChar())
            Int::class -> unsafe.putInt(elementAddress, value as? Int ?: 0)
            Long::class -> unsafe.putLong(elementAddress, value as? Long ?: 0)
            Float::class -> unsafe.putFloat(elementAddress, value as? Float ?: 0f)
            Double::class -> unsafe.putDouble(elementAddress, value as? Double ?: 0.0)
            else -> error("Cannot set element ${elementInfo.type.simpleName} in array.")
        }
    }

    @Suppress("Unchecked_Cast")
    override operator fun get(index: Int): E? {
        if (index < 0 || index > length)
            throw IndexOutOfBoundsException(index)

        var elementAddress = elementBase + (index * elementSize).toLong()
        if (elementInfo.isPointer || elementInfo.isArray)
            elementAddress = unsafe.getAddress(elementAddress)

        if (elementAddress == 0L)
            return null

        if (factory != null)
            return factory!!(elementAddress)

        return when (elementInfo.type) {
            Byte::class -> unsafe.getByte(elementAddress)
            Short::class -> unsafe.getShort(elementAddress)
            Char::class -> unsafe.getChar(elementAddress)
            Int::class -> unsafe.getInt(elementAddress)
            Long::class -> unsafe.getLong(elementAddress)
            Float::class -> unsafe.getFloat(elementAddress)
            Double::class -> unsafe.getDouble(elementAddress)
            else -> error("Cannot get element ${elementInfo.type.simpleName} from array.")
        } as E
    }

    class ArrayIterator<T : Any>(private val array: NArray<T>) : Iterator<T> {
        private var index = 0

        override fun next() = array[index++] ?: error("Unreachable")
        override fun hasNext() = index < array.length
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "NArray@0x${address.base.toHexString()} (${elementInfo})"

    override fun iterator() = ArrayIterator(this)
}

abstract class NPrimitiveArray<E : Any>(address: Address, info: ElementInfo) : NArray<E>(address, info), NPrimitiveArrayApi<E> {
    override operator fun get(index: Int): E = super.get(index)!!
}

class NByteArray(address: Address, info: ElementInfo) : NPrimitiveArray<Byte>(address, info)
class NLongArray(address: Address, info: ElementInfo) : NPrimitiveArray<Long>(address, info)
class NIntArray(address: Address, info: ElementInfo) : NPrimitiveArray<Int>(address, info)
class NShortArray(address: Address, info: ElementInfo) : NPrimitiveArray<Short>(address, info)