package land.src.jvmtb.jvm

class Field(
    val name: String,
    val typeName: String,
    val offsetOrAddress: Long,
    val isStatic: Boolean
) : Comparable<Field> {
    override fun compareTo(other: Field): Int {
        if (isStatic != other.isStatic)
            return if (isStatic) -1 else 1

        return offsetOrAddress.compareTo(other.offsetOrAddress)
    }

// fun getString(offset: Long = 0) = vm.getString(offsetOrAddress + offset)
// fun getStringRef(offset: Long = 0) = vm.getStringRef(offsetOrAddress + offset)

// fun getByte(offset: Long = 0) = vm.getByte(offsetOrAddress + offset)
// fun putByte(value: Byte, offset: Long = 0) = vm.putByte(offsetOrAddress + offset, value)
// fun getShort(offset: Long = 0) = vm.getShort(offsetOrAddress + offset)
// fun putShort(value: Short, offset: Long = 0) = vm.putShort(offsetOrAddress + offset, value)
// fun getChar(offset: Long = 0) = vm.getChar(offsetOrAddress + offset)
// fun putChar(value: Char, offset: Long = 0) = vm.putChar(offsetOrAddress + offset, value)
// fun getInt(offset: Long = 0) = vm.getInt(offsetOrAddress + offset)
// fun putInt(value: Int, offset: Long = 0) = vm.putInt(offsetOrAddress + offset, value)
// fun getLong(offset: Long = 0) = vm.getLong(offsetOrAddress + offset)
// fun putLong(value: Long, offset: Long = 0) = vm.putLong(offsetOrAddress + offset, value)
// fun getFloat(offset: Long = 0) = vm.getFloat(offsetOrAddress + offset)
// fun putFloat(value: Float, offset: Long = 0) = vm.putFloat(offsetOrAddress + offset, value)
// fun getDouble(offset: Long = 0) = vm.getDouble(offsetOrAddress + offset)
// fun putDouble(value: Double, offset: Long = 0) = vm.putDouble(offsetOrAddress + offset, value)
// fun getAddress(offset: Long = 0) = vm.getAddress(offsetOrAddress + offset)
// fun putAddress(value: Long, offset: Long = 0) = vm.putAddress(offsetOrAddress + offset, value)
// fun getMemory(length: Int, offset: Long = 0) = vm.getMemory(offsetOrAddress + offset, length)
// fun putMemory(bytes: ByteArray, offset: Long = 0) = vm.putMemory(offsetOrAddress + offset, bytes)
}