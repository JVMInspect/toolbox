package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.offset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class Symbol(address: Address) : Struct(address), Oop {
    private val _body by offset("_body")
    private var _length: Short by nonNull("_length")

    val length = _length.toInt() and 0xffff
    val bytes = unsafe.getMemory(address.base + _body, length)
    val string = readModifiedUTF8(bytes)

    private fun readModifiedUTF8(buf: ByteArray): String {
        val len = buf.size
        val tmp = ByteArray(len + 2)
        tmp[0] = ((len ushr 8) and 0xFF).toByte()
        tmp[1] = ((len ushr 0) and 0xFF).toByte()
        System.arraycopy(buf, 0, tmp, 2, len)
        val dis = DataInputStream(ByteArrayInputStream(tmp))
        return dis.readUTF()
    }

    companion object {
        fun create(string: String, scope: Scope): Symbol {
            val bytes = string.encodeToByteArray()
            val length = bytes.size

            val type = scope.vm.type("Symbol")
            val body = type.field("_body")!!.offsetOrAddress
            val lengthOffset = type.field("_length")!!.offsetOrAddress

            val address = scope.unsafe.allocateMemory((body + length))
            scope.unsafe.putShort(address + lengthOffset, length.toShort())
            scope.unsafe.putMemory(address + body, bytes)

            val symbol: Symbol = scope.oops(address)!!

            return symbol
        }
    }
}