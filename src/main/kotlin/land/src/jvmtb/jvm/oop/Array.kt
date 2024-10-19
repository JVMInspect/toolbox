package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.cache.Arrays
import land.src.jvmtb.util.isStruct
import land.src.jvmtb.util.sizeOf
import kotlin.reflect.KClass

private fun <T : Any> VMScope.getTypeName(elementType: KClass<T>): String {
    if (elementType.isStruct)
        return structs.nameOf(elementType)

    return when (elementType) {
        Byte::class -> "u1"
        Short::class, Char::class -> "u2"
        Int::class -> "int"
        else -> error("Cannot get element ${elementType.simpleName} from array.")
    }
}

class Array<T : Any>(private val elementType: KClass<T>, address: Address) : Struct(address), Iterable<T> {
    val length get() = unsafe.getInt(address.base)
    private val elementBase get() = type.field("_data").offsetOrAddress
    private val elementSize get() = sizeOf(elementType)

    // write all content to byte array
    val bytes: ByteArray get() = TODO()

    override val typeName = "Array<${getTypeName(elementType)}>"

    @Suppress("Unchecked_Cast")
    operator fun get(index: Int): T {
        val elementAddress = address.base + elementBase + index.toLong() * elementSize
        if (elementType.isStruct)
            return address.scope.structs(elementType, elementAddress) as T

        return when (elementType) {
            Byte::class -> unsafe.getByte(elementAddress)
            Short::class -> unsafe.getShort(elementAddress)
            Char::class -> unsafe.getChar(elementAddress)
            Int::class -> unsafe.getInt(elementAddress)
            Long::class -> unsafe.getLong(elementAddress)
            Float::class -> unsafe.getFloat(elementAddress)
            Double::class -> unsafe.getDouble(elementAddress)
            else -> error("Cannot get element ${elementType.simpleName} from array.")
        } as T
    }

    class ArrayIterator<T : Any>(private val array: Array<T>) : Iterator<T> {
        private var index = 0

        override fun next() = array[index++]
        override fun hasNext() = index < array.length
    }

    override fun iterator() = ArrayIterator(this)
}