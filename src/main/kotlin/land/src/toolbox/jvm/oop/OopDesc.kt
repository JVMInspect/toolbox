package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.address
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct

class OopDesc(address: Address) : Struct(address) {
    override val typeName: String = "oopDesc"

    val _klass: Klass by nonNull("_metadata._klass")

    var useCompressedKlassPointers = true

    val klass: Klass get() {
        if (!useCompressedKlassPointers)
            return _klass

        val narrowKlass = _klass.base
        val narrowKlassBase = globals.compressedKlassPointers.narrowKlassBase
        val narrowKlassShift = globals.compressedKlassPointers.narrowKlassShift
        val klass = narrowKlassBase + (narrowKlass shl narrowKlassShift)

        return structs<Klass>(klass)!!
    }

    inline fun <reified V : Any> getField(offset: Int): V {
        val address = address.base + offset
        return when (V::class) {
            Boolean::class -> unsafe.getByte(address) != 0.toByte()
            Byte::class -> unsafe.getByte(address)
            Short::class -> unsafe.getShort(address)
            Char::class -> unsafe.getChar(address)
            Int::class -> unsafe.getInt(address)
            Long::class -> unsafe.getLong(address)
            Float::class -> unsafe.getFloat(address)
            Double::class -> unsafe.getDouble(address)
            Oop::class -> Oop(Address(this, unsafe.getAddress(address)))
            else -> error("${V::class.simpleName} getter is not supported")
        } as V
    }

    inline fun <reified V : Any> setField(offset: Int, value: V) {
        val address = address.base + offset
        when (V::class) {
            Boolean::class -> unsafe.putByte(address, if (value as Boolean) 1 else 0)
            Byte::class -> unsafe.putByte(address, value as Byte)
            Short::class -> unsafe.putShort(address, value as Short)
            Char::class -> unsafe.putChar(address, value as Char)
            Int::class -> unsafe.putInt(address, value as Int)
            Long::class -> unsafe.putLong(address, value as Long)
            Float::class -> unsafe.putFloat(address, value as Float)
            Double::class -> unsafe.putDouble(address, value as Double)
            Oop::class -> unsafe.putAddress(address, (value as Oop).base)
            else -> error("${V::class.simpleName} setter is not supported")
        }
    }
}