package land.src.toolbox.remote

import com.sun.jna.Memory
import land.src.toolbox.util.pointer

class RemoteUnsafe(private val process: RemoteProcess) {
    private val memory = Memory(1024 * 1024L)

    fun getString(address: Long): String? {
        if (address == 0L) return null

        var buffer = CharArray(32)
        var offset = 0

        while (true) {
            val b = getByte(address + offset)
            if (b == 0.toByte()) break

            if (offset >= buffer.size) {
                buffer = buffer.copyOf(offset * 2)
            }

            buffer[offset++] = b.toInt().toChar()
        }

        return String(buffer, 0, offset)
    }

    fun getByte(address: Long): Byte {
        check(process.read(address.pointer, memory, Byte.SIZE_BYTES) == Byte.SIZE_BYTES) {
            "getByte"
        }
        return memory.getByte(0)
    }

    fun putByte(address: Long, x: Byte) {
        memory.write(0, byteArrayOf(x), 0, 1)
        check(process.write(address.pointer, memory, Byte.SIZE_BYTES) == Byte.SIZE_BYTES) {
            "putByte"
        }
    }

    fun getShort(address: Long): Short {
        check(process.read(address.pointer, memory, Short.SIZE_BYTES) == Short.SIZE_BYTES) {
            "getShort"
        }
        return memory.getShort(0)
    }

    fun putShort(address: Long, x: Short) {
        memory.write(0, shortArrayOf(x), 0, 1)
        check(process.write(address.pointer, memory, Short.SIZE_BYTES) == Short.SIZE_BYTES) {
            "putShort"
        }
    }

    fun getChar(address: Long): Char {
        check(process.read(address.pointer, memory, Char.SIZE_BYTES) == Char.SIZE_BYTES) {
            "getChar"
        }
        return memory.getChar(0)
    }

    fun putChar(address: Long, x: Char) {
        memory.write(0, charArrayOf(x), 0, 1)
        check(process.write(address.pointer, memory, Char.SIZE_BYTES) == Char.SIZE_BYTES) {
            "putChar"
        }
    }

    fun getInt(address: Long): Int {
        check(process.read(address.pointer, memory, Int.SIZE_BYTES) == Int.SIZE_BYTES) {
            "getInt"
        }
        return memory.getInt(0)
    }

    fun putInt(address: Long, x: Int) {
        memory.write(0, intArrayOf(x), 0, 1)
        check(process.write(address.pointer, memory, Int.SIZE_BYTES) == Int.SIZE_BYTES) {
            "putInt"
        }
    }

    fun getLong(address: Long): Long {
        check(process.read(address.pointer, memory, Long.SIZE_BYTES) == Long.SIZE_BYTES) {
            "getLong"
        }
        return memory.getLong(0)
    }

    fun putLong(address: Long, x: Long) {
        memory.write(0, longArrayOf(x), 0, 1)
        check(process.write(address.pointer, memory, Long.SIZE_BYTES) == Long.SIZE_BYTES) {
            "putLong"
        }
    }

    fun getFloat(address: Long): Float {
        check(process.read(address.pointer, memory, Float.SIZE_BYTES) == Float.SIZE_BYTES) {
            "getFloat"
        }
        return memory.getFloat(0)
    }

    fun putFloat(address: Long, x: Float) {
        memory.write(0, floatArrayOf(x), 0, 1)
        check(process.write(address.pointer, memory, Float.SIZE_BYTES) == Float.SIZE_BYTES) {
            "putFloat"
        }
    }

    fun getDouble(address: Long): Double {
        check(process.read(address.pointer, memory, Double.SIZE_BYTES) == Double.SIZE_BYTES) {
            "getDouble"
        }
        return memory.getDouble(0)
    }

    fun putDouble(address: Long, x: Double) {
        memory.write(0, doubleArrayOf(x), 0, 1)
        check(process.write(address.pointer, memory, Double.SIZE_BYTES) == Double.SIZE_BYTES) {
            "putDouble"
        }
    }

    fun getAddress(address: Long): Long {
        check(process.read(address.pointer, memory, Long.SIZE_BYTES) == Long.SIZE_BYTES) {
            "getAddress"
        }
        return memory.getLong(0)
    }

    fun putAddress(address: Long, x: Long) {
        memory.write(0, longArrayOf(x), 0, 1)
        check(process.write(address.pointer, memory, Long.SIZE_BYTES) == Long.SIZE_BYTES) {
            "putAddress"
        }
    }

    fun getMemory(address: Long, length: Int): ByteArray {
        check(process.read(address.pointer, memory, length) == length) {
            "getMemory"
        }
        return memory.getByteArray(0, length)
    }

    fun putMemory(address: Long, bytes: ByteArray) {
        memory.write(0, bytes, 0, bytes.size)
        check(process.write(address.pointer, memory, bytes.size) == bytes.size) {
            "setMemory"
        }
    }
}