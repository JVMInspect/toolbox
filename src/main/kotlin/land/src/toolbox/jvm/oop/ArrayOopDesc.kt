package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.util.*
import land.src.toolbox.jvm.util.roundTo
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun elemBytes(type: Int): Int {
    return when (type) {
        T_BYTE -> 1
        T_CHAR -> 2
        T_SHORT -> 2
        T_INT -> 4
        T_LONG -> 8
        T_FLOAT -> 4
        T_DOUBLE -> 8
        T_OBJECT -> 8
        T_NARROWOOP -> 4
        T_NARROWKLASS -> 4
        else -> 8
    }
}

class ArrayOopDesc(address: Address) : OopDesc(address) {
    override val typeName: String = "arrayOopDesc"

    var length: Int get() {
        // when compressed class pointers is used the length is stored in the top half of the _metadata._narrow_klass field
        return if (useCompressedKlassPointers) {
            unsafe.getInt(address.base + klassGap)
        } else {
            // otherwise the length is stored right after the header
            getField<Int>(structs.sizeof(ArrayOopDesc::class))
        }
    }
    set(value) {
        if (useCompressedKlassPointers) {
            unsafe.putInt(address.base + klassGap, value)
        } else {
            setField(type.size, value)
        }
    }

    private val headerSize: Int by lazy {
        type.size
    }

    fun headerSize(type: Int): Int {
        val header = headerSize
        if (elementShouldBeAligned(type)) {
            return roundTo(header, 8)
        }
        return header
    }

    private fun elementShouldBeAligned(type: Int): Boolean {
        return type == T_DOUBLE || type == T_LONG
    }

    fun mapType(type: KClass<*>): Int {
        return when {
            type == Byte::class -> T_BYTE
            type == Char::class -> T_CHAR
            type == Short::class -> T_SHORT
            type == Int::class -> T_INT
            type == Long::class -> T_LONG
            type == Float::class -> T_FLOAT
            type == Double::class -> T_DOUBLE
            type.isSubclassOf(OopDesc::class) -> if (useCompressedOops) T_NARROWOOP else T_OBJECT
            type.isSubclassOf(Klass::class) -> if (useCompressedKlassPointers) T_NARROWKLASS else T_OBJECT
            else -> T_OBJECT
        }
    }

    fun arraySize(type: Int): Int {
        return length * elemBytes(type) + headerSize(type)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun expand(type: Int, by: Int): ArrayOopDesc {
        val oldSize = arraySize(type)
        val newSize = oldSize + by * elemBytes(type)
        // allocating new memory will cause this array to no longer be valid, this will cause a memory zap
        // filling this arrays memory with BAADBABE, therefore we need to save the memory here
        val savedMemory = unsafe.getMemory(address.base, oldSize)
        val oldLength = length
        val newAddress = objects.allocateMemory(newSize)
        println("New array address: ${newAddress.toHexString()}")

        val newArray = ArrayOopDesc(Address(this, newAddress))
        unsafe.putMemory(newAddress, savedMemory)
        newArray.length = oldLength + by

        return newArray
    }

    inline operator fun <reified T: Any> get(index: Int): T {
        val type = mapType(T::class)
        val base = headerSize(type)
        val bytes = elemBytes(type)
        return getField<T>(base + index * bytes)
    }

    inline operator fun <reified T: Any> set(index: Int, value: T) {
        val type = mapType(T::class)
        val base = headerSize(type)
        val bytes = elemBytes(type)
        setField(base + index * bytes, value)
    }
}