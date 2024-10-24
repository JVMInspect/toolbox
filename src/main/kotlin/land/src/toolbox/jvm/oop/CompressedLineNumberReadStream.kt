package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.util.Unsigned5
import land.src.toolbox.jvm.util.Unsigned5.readUint


class CompressedLineNumberReadStream(val address: Address) : Iterator<LineNumberTableElement>, Scope by address {
    var bci = 0
    var line = 0
    var position = 0

    fun decodeSign(value: Int): Int {
        return (value ushr 1) xor -(value and 1)
    }

    fun readByte(increment: Boolean = true): Byte {
        val byte = unsafe.getByte(address.base + position)
        if (increment)
            position++
        return (byte.toInt() and 0xff).toByte()
    }

    fun readSignedInt(): Int {
        val int = readUint(address, position) {
            this.position = position
            position
        }
        //val int = unsafe.getInt(address.base + position) and 0xff
        //position += 4
        return decodeSign(int.toInt())
    }

    /**
     *     return (int) Unsigned5.readUint(this, position,
     *                                     // bytes are fetched here:
     *                                     CompressedReadStream::read,
     *                                     // updated position comes through here:
     *                                     CompressedReadStream::setPosition);
     */



    // todo
    override fun hasNext(): Boolean =
        false
        //readByte(increment = false).toInt() and 0xff != 0

    override fun next(): LineNumberTableElement {
        val next = readByte().toInt() and 0xFF
        if (next == 0xFF) {
            // Escape character, regular compression used
            bci  += readSignedInt()
            line += readSignedInt()
            println("Added regular entry $bci, $line")
        } else {
            // Single byte compression used
            bci  += next shr 3
            line += next and 0x7
            println("Added compressed entry $bci, $line")
        }
        return LineNumberTableElement(bci.toShort(), line.toShort())
    }
}