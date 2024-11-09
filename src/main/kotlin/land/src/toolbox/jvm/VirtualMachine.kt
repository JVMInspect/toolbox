package land.src.toolbox.jvm

import com.sun.jna.Pointer
import land.src.toolbox.jvm.cache.Arrays
import land.src.toolbox.jvm.cache.Fields
import land.src.toolbox.jvm.cache.Oops
import land.src.toolbox.jvm.cache.Structs
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Field
import land.src.toolbox.jvm.primitive.Type
import land.src.toolbox.process.ProcessHandle
import java.io.File
import java.util.*

class VirtualMachine(private val process: ProcessHandle, private val structsFile: File? = null) : Scope {
    private val symbols = Symbols(process)
    private val constants = LinkedHashMap<String, Number>()

    val isLocal = process.local
    private var vmTypes: VMStructs

    override val vm = this
    override val version: VMVersion
    override val unsafe = process.unsafe
    override val globals = Globals(this)
    override val oops = Oops(this)
    override val arrays = Arrays(this)
    override val structFields = Fields(this)
    override val structs = Structs(this)
    override val universe = Universe(this)
    override val vmClasses = VMClasses(this)
    override val objects = Objects(this)

    init {
        // either read the structs from the file or from the process
        vmTypes = if (structsFile != null) {
            VMStructs().apply {
                parse(structsFile.readText())
            }
        } else {
            VMStructs(readVmTypes(readVmStructs()))
        }
        readVmIntConstants()
        readVmLongConstants()
        version = structs(Address.PLACEHOLDER)!!
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
            val typeNameAddress = unsafe.getAddress(entry + typeNameOffset)
            val typeName = unsafe.getString(typeNameAddress) ?: break
            val fieldNameAddress = unsafe.getAddress(entry + fieldNameOffset)
            val fieldName = unsafe.getString(fieldNameAddress) ?: ""
            val typeStringAddress = unsafe.getAddress(entry + typeStringOffset)
            val typeString = unsafe.getString(typeStringAddress) ?: ""

            val fields = structs.computeIfAbsent(typeName) { TreeSet<Field>() }

            val isStatic = unsafe.getInt(entry + isStaticOffset) != 0
            val offsetOrAddress = unsafe.getLong(entry + if (isStatic) addressOffset else offsetOffset)

            fields.add(Field(fieldName, typeString, offsetOrAddress, isStatic))
            entry += arrayStride
        }

        return structs
    }

    private fun readVmTypes(structs: Map<String, Set<Field>>): MutableMap<String, Type> {
        var entry = symbol("gHotSpotVMTypes")
        val typeNameOffset = symbol("gHotSpotVMTypeEntryTypeNameOffset")
        val superclassNameOffset = symbol("gHotSpotVMTypeEntrySuperclassNameOffset")
        val isOopTypeOffset = symbol("gHotSpotVMTypeEntryIsOopTypeOffset")
        val isIntegerTypeOffset = symbol("gHotSpotVMTypeEntryIsIntegerTypeOffset")
        val isUnsignedOffset = symbol("gHotSpotVMTypeEntryIsUnsignedOffset")
        val sizeOffset = symbol("gHotSpotVMTypeEntrySizeOffset")
        val arrayStride = symbol("gHotSpotVMTypeEntryArrayStride")

        val types = mutableMapOf<String, Type>()

        while (true) {
            val typeNameAddress = unsafe.getAddress(entry + typeNameOffset)
            val typeName = unsafe.getString(typeNameAddress) ?: break
            val superclassNameAddress = unsafe.getAddress(entry + superclassNameOffset)
            val superclassName = unsafe.getString(superclassNameAddress) ?: ""

            val size = unsafe.getInt(entry + sizeOffset)
            val isOop = unsafe.getInt(entry + isOopTypeOffset) != 0
            val isInt = unsafe.getInt(entry + isIntegerTypeOffset) != 0
            val isUnsigned = unsafe.getInt(entry + isUnsignedOffset) != 0

            val fields = structs[typeName] ?: emptySet()
            types[typeName] = Type(typeName, superclassName, size, isOop, isInt, isUnsigned, fields)
            entry += arrayStride
        }

        return types
    }

    private fun readVmIntConstants() {
        var entry = symbol("gHotSpotVMIntConstants")
        val nameOffset = symbol("gHotSpotVMIntConstantEntryNameOffset")
        val valueOffset = symbol("gHotSpotVMIntConstantEntryValueOffset")
        val arrayStride = symbol("gHotSpotVMIntConstantEntryArrayStride")

        while (true) {
            val nameAddress = unsafe.getAddress(entry + nameOffset)
            val name = unsafe.getString(nameAddress) ?: break

            constants[name] = unsafe.getInt(entry + valueOffset)
            entry += arrayStride
        }
    }

    private fun readVmLongConstants() {
        var entry = symbol("gHotSpotVMLongConstants")
        val nameOffset = symbol("gHotSpotVMLongConstantEntryNameOffset")
        val valueOffset = symbol("gHotSpotVMLongConstantEntryValueOffset")
        val arrayStride = symbol("gHotSpotVMLongConstantEntryArrayStride")

        while (true) {
            val nameAddress = unsafe.getAddress(entry + nameOffset)
            val name = unsafe.getString(nameAddress) ?: break

            constants[name] = unsafe.getLong(entry + valueOffset)
            entry += arrayStride
        }
    }

    fun print() {
        for ((key, value) in constants) {
            val type = if (value is Long) "long" else "int"
            println("const $type $key = $value")
        }
        println()

        println("Types:")
        println(vmTypes.print())
    }

    fun print(file: File) {
        file.printWriter().use { out ->
            out.println(vmTypes.print())
        }
    }

    val is64Bit by lazy {
        process.is64Bit()
    }

    private fun symbol(name: String): Long =
        unsafe.getLong(Pointer.nativeValue(symbols.lookup(name)))

    fun type(name: String) = vmTypes[name]
        ?: throw NoSuchElementException("No such type: $name")

    fun constant(name: String) = constants[name]
        ?: throw NoSuchElementException("No such constant: $name")
}