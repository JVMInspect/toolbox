package land.src.jvmtb.jvm

import com.sun.jna.Pointer
import land.src.jvmtb.jvm.cache.Arrays
import land.src.jvmtb.jvm.cache.OopCache
import land.src.jvmtb.jvm.cache.StructCache
import land.src.jvmtb.remote.RemoteProcess
import java.util.*

class VirtualMachine(private val process: RemoteProcess) : VMScope {
    private val symbols = Symbols(process)
    private val types = LinkedHashMap<String, Type>()
    private val constants = LinkedHashMap<String, Number>()

    override val vm = this
    override val unsafe = process.unsafe
    override val oops = OopCache(this)
    override val arrays = Arrays(this)
    override val structs = StructCache(this)

    init {
        readVmTypes(readVmStructs())
        readVmIntConstants()
        readVmLongConstants()
    }

    private fun readVmStructs(): Map<String, MutableSet<Field>> {
        var entry = symbol("gHotSpotVMStructs")
        val typeNameOffset = symbol("gHotSpotVMStructEntryTypeNameOffset")
        val fieldNameOffset = symbol("gHotSpotVMStructEntryFieldNameOffset")
        val typeStringOffset = symbol("gHotSpotVMStructEntryTypeStringOffset")
        val isStaticOffset = symbol("gHotSpotVMStructEntryIsStaticOffset")
        val offsetOffset = symbol("gHotSpotVMStructEntryOffsetOffset")
        val addressOffset = symbol("gHotSpotVMStructEntryAddressOffset")
        val arrayStride = symbol("gHotSpotVMStructEntryArrayStride")

        val structs = HashMap<String, MutableSet<Field>>()

        while (true) {
            val typeName = getStringRef(entry + typeNameOffset) ?: break
            val fieldName = getStringRef(entry + fieldNameOffset) ?: ""
            val typeString = getStringRef(entry + typeStringOffset) ?: ""

            val fields = structs.computeIfAbsent(typeName) { TreeSet<Field>() }

            val isStatic = getInt(entry + isStaticOffset) != 0
            val offsetOrAddress = getLong(entry + if (isStatic) addressOffset else offsetOffset)

            fields.add(Field(fieldName, typeString, offsetOrAddress, isStatic))
            entry += arrayStride
        }

        return structs
    }

    private fun readVmTypes(structs: Map<String, Set<Field>>) {
        var entry = symbol("gHotSpotVMTypes")
        val typeNameOffset = symbol("gHotSpotVMTypeEntryTypeNameOffset")
        val superclassNameOffset = symbol("gHotSpotVMTypeEntrySuperclassNameOffset")
        val isOopTypeOffset = symbol("gHotSpotVMTypeEntryIsOopTypeOffset")
        val isIntegerTypeOffset = symbol("gHotSpotVMTypeEntryIsIntegerTypeOffset")
        val isUnsignedOffset = symbol("gHotSpotVMTypeEntryIsUnsignedOffset")
        val sizeOffset = symbol("gHotSpotVMTypeEntrySizeOffset")
        val arrayStride = symbol("gHotSpotVMTypeEntryArrayStride")

        while (true) {
            val typeName = getStringRef(entry + typeNameOffset) ?: break
            val superclassName = getStringRef(entry + superclassNameOffset) ?: ""

            val size = getInt(entry + sizeOffset)
            val isOop = getInt(entry + isOopTypeOffset) != 0
            val isInt = getInt(entry + isIntegerTypeOffset) != 0
            val isUnsigned = getInt(entry + isUnsignedOffset) != 0

            val fields = structs[typeName] ?: emptySet()
            types[typeName] = Type(typeName, superclassName, size, isOop, isInt, isUnsigned, fields)
            entry += arrayStride
        }
    }

    private fun readVmIntConstants() {
        var entry = symbol("gHotSpotVMIntConstants")
        val nameOffset = symbol("gHotSpotVMIntConstantEntryNameOffset")
        val valueOffset = symbol("gHotSpotVMIntConstantEntryValueOffset")
        val arrayStride = symbol("gHotSpotVMIntConstantEntryArrayStride")

        while (true) {
            val name = getStringRef(entry + nameOffset) ?: break

            constants[name] = getInt(entry + valueOffset)
            entry += arrayStride
        }
    }

    private fun readVmLongConstants() {
        var entry = symbol("gHotSpotVMLongConstants")
        val nameOffset = symbol("gHotSpotVMLongConstantEntryNameOffset")
        val valueOffset = symbol("gHotSpotVMLongConstantEntryValueOffset")
        val arrayStride = symbol("gHotSpotVMLongConstantEntryArrayStride")

        while (true) {
            val name = getStringRef(entry + nameOffset) ?: break

            constants[name] = getLong(entry + valueOffset)
            entry += arrayStride
        }
    }

    fun is64Bit() = process.is64Bit()

    private fun symbol(name: String): Long =
        getLong(Pointer.nativeValue(symbols.lookup(name)))

    fun type(name: String) = types[name]
        ?: throw NoSuchElementException("No such type: $name")

    fun constant(name: String) = constants[name]
        ?: throw NoSuchElementException("No such constant: $name")

    fun intConstant(name: String) = constant(name).toInt()
    fun longConstant(name: String) = constant(name).toLong()

    fun getString(address: Long) = process.unsafe.getString(address)
    fun getStringRef(address: Long) = getString(getAddress(address))

    fun getByte(address: Long) = process.unsafe.getByte(address)
    fun putByte(address: Long, x: Byte) = process.unsafe.putByte(address, x)
    fun getShort(address: Long) = process.unsafe.getShort(address)
    fun putShort(address: Long, x: Short) = process.unsafe.putShort(address, x)
    fun getChar(address: Long) = process.unsafe.getChar(address)
    fun putChar(address: Long, x: Char) = process.unsafe.putChar(address, x)
    fun getInt(address: Long) = process.unsafe.getInt(address)
    fun putInt(address: Long, x: Int) = process.unsafe.putInt(address, x)
    fun getLong(address: Long) = process.unsafe.getLong(address)
    fun putLong(address: Long, x: Long) = process.unsafe.putLong(address, x)
    fun getFloat(address: Long) = process.unsafe.getFloat(address)
    fun putFloat(address: Long, x: Float) = process.unsafe.putFloat(address, x)
    fun getDouble(address: Long) = process.unsafe.getDouble(address)
    fun putDouble(address: Long, x: Double) = process.unsafe.putDouble(address, x)
    fun getAddress(address: Long) = process.unsafe.getAddress(address)
    fun putAddress(address: Long, x: Long) = process.unsafe.putAddress(address, x)
    fun getMemory(address: Long, length: Int) = process.unsafe.getMemory(address, length)
    fun putMemory(address: Long, bytes: ByteArray) = process.unsafe.putMemory(address, bytes)
}