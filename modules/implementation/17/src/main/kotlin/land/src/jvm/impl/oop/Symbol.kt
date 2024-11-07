package land.src.jvm.impl.oop

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.offset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import land.src.jvm.api.oop.pool.Symbol as SymbolApi

class Symbol(address: Address) : Struct(address), Oop, SymbolApi {
    private val _body by offset("_body")
    private var _length: Short by nonNull("_length")

    override val length = _length.toInt() and 0xffff
    override val bytes = unsafe.getMemory(address.base + _body, length)
    override val string = readModifiedUTF8(bytes)

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
        fun create(string: ByteArray, scope: Scope): Symbol {
            val newSize = scope.structs.sizeof(Symbol::class) + string.size
            val address = scope.unsafe.allocateMemory(newSize.toLong())

            val symbol: Symbol = scope.oops(address)!!

            symbol._length = string.size.toShort()

            scope.unsafe.putMemory(address + symbol._body, string)

            return symbol
        }
    }
}