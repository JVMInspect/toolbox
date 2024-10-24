package land.src.toolbox.jvm.primitive

import land.src.toolbox.jvm.Scope
import kotlin.reflect.KClass

open class Array<E : Any>(address: Address, private val elementInfo: ElementInfo) : Struct(address), Iterable<E> {
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

    val length by lazy {
        unsafe.getInt(address.base)
    }

    val bytes by lazy {
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

    @Suppress("Unchecked_Cast")
    operator fun get(index: Int): E? {
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

    class ArrayIterator<T : Any>(private val array: Array<T>) : Iterator<T> {
        private var index = 0

        override fun next() = array[index++] ?: error("Unreachable")
        override fun hasNext() = index < array.length
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "Array@0x${address.base.toHexString()} (${elementInfo})"

    override fun iterator() = ArrayIterator(this)
}

class ByteArray(address: Address, info: ElementInfo) : Array<Byte>(address, info)
class ShortArray(address: Address, info: ElementInfo) : Array<Short>(address, info)