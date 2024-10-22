package land.src.jvmtb.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.offset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class Symbol(address: Address) : Oop(address) {
    private val _body by offset("_body")
    private val _length: Short by nonNull("_length")

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
}