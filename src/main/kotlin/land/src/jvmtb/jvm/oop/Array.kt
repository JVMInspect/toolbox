package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.util.isArray
import land.src.jvmtb.util.isStruct
import land.src.jvmtb.util.sizeOf
import kotlin.reflect.KClass
import kotlin.reflect.javaType

class Array<E : Any>(private val elementType: KClass<E>, private val isElementPointer: Boolean, address: Address) : Struct(address), Iterable<E> {
    val length get() = unsafe.getInt(address.base)
    val elementBase get() = type.field("_data[0]").offsetOrAddress
    val elementSize get() = if (isElementPointer) 8 else sizeOf(elementType)

    fun getAddress(index: Int): Long {
        return unsafe.getAddress(elementBase + index.toLong() * elementSize)
    }

    @Suppress("Unchecked_Cast")
    @OptIn(ExperimentalStdlibApi::class)
    operator fun get(index: Int): E {
        val element = address.base + elementBase + (index.toLong() * elementSize)
        val elementAddress = if (isElementPointer || elementType.isArray) unsafe.getLong(element) else element

        if (elementType.isArray) {
            val parameters = elementType.typeParameters[0]
            val arrayElementType = parameters.upperBounds[0].javaType as Class<*>
            // an array of arrays will always have the elements (arrays) be a pointer, so we can use the isElementPointer
            // field to deduce if the elements of the sub-array type are pointers.
            return arrays(arrayElementType.kotlin, elementType, isElementPointer, elementAddress) as E
        }

        if (elementType.isStruct)
            return address.scope.structs(elementType, elementAddress) as E

        return when (elementType) {
            Byte::class -> unsafe.getByte(elementAddress)
            Short::class -> unsafe.getShort(elementAddress)
            Char::class -> unsafe.getChar(elementAddress)
            Int::class -> unsafe.getInt(elementAddress)
            Long::class -> unsafe.getLong(elementAddress)
            Float::class -> unsafe.getFloat(elementAddress)
            Double::class -> unsafe.getDouble(elementAddress)
            else -> error("Cannot get element ${elementType.simpleName} from array.")
        } as E
    }

    class ArrayIterator<T : Any>(private val array: Array<T>) : Iterator<T> {
        private var index = 0

        override fun next() = array[index++]
        override fun hasNext() = index < array.length
    }

    override fun iterator() = ArrayIterator(this)
}

val Array<*>.bytes get() =
    unsafe.getMemory(elementBase, length * elementSize)