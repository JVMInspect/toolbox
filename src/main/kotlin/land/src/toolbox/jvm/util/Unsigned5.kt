package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Address

object Unsigned5 {
    const val LogBitsPerByte = 3
    const val BitsPerByte = 1 shl 3

    // Constants for UNSIGNED5 coding of Pack200
    private const val lg_H = 6     // log-base-2 of H (lg 64 == 6)
    private const val H = 1 shl lg_H  // number of "high" bytes (64)
    private const val X = 1  // there is one excluded byte ('\0')
    private const val MAX_b = (1 shl BitsPerByte) - 1  // largest byte value
    private const val L = (MAX_b + 1) - X - H  // number of "low" bytes (191)
    const val MAX_LENGTH = 5  // lengths are in [1..5]

    // Temporary functions for `getByte` and `setPosition`
    private fun Scope.getByte(array: Address, position: Int): Int {
        val byte = unsafe.getByte(array.base + position)
        return byte.toInt() and 0xFF // To get an unsigned byte
    }

    fun readUint(base: Address, position: Int, setPosition: (pos: Int) -> Int): Long {
        var pos = position
        val b0 = base.getByte(base, pos)
        var sum = b0 - X
        if (sum < L) {  // common case
            pos = setPosition(pos + 1)
            return sum.toLong()
        }

        var lg_H_i = lg_H  // lg(H)*i == lg(H^^i)
        for (i in 1 until MAX_LENGTH) {
            val b_i = base.getByte(base, pos + i)
            if (b_i < X) {
                pos = setPosition(pos + i)
                return sum.toLong()
            }
            sum += (b_i - X) shl lg_H_i  // sum += (b[i]-X)*(64^^i)
            if (b_i < X + L || i == MAX_LENGTH - 1) {
                pos = setPosition(pos + i + 1)
                return sum.toLong()
            }
            lg_H_i += lg_H
        }
        return sum.toLong()
    }
}