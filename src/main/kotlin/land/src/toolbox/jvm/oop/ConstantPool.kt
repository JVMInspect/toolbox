package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.maybeNullArray
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.nonNullArray
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import land.src.toolbox.jvm.util.*
import software.coley.cafedude.classfile.constant.ConstRef
import software.coley.cafedude.classfile.constant.CpClass
import software.coley.cafedude.classfile.constant.CpDouble
import software.coley.cafedude.classfile.constant.CpDynamic
import software.coley.cafedude.classfile.constant.CpEntry
import software.coley.cafedude.classfile.constant.CpFloat
import software.coley.cafedude.classfile.constant.CpInt
import software.coley.cafedude.classfile.constant.CpInvokeDynamic
import software.coley.cafedude.classfile.constant.CpLong
import software.coley.cafedude.classfile.constant.CpMethodRef
import software.coley.cafedude.classfile.constant.CpModule
import software.coley.cafedude.classfile.constant.CpNameType
import software.coley.cafedude.classfile.constant.CpPackage
import software.coley.cafedude.classfile.constant.CpString
import software.coley.cafedude.classfile.constant.CpUtf8
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

class ConstantPool(address: Address) : Struct(address), Oop {
    var length: Int by nonNull("_length")
    val majorVersion: Short by nonNull("_major_version")
    val minorVersion: Short by nonNull("_minor_version")
    var tags: Array<Byte> by nonNullArray("_tags")
    val poolHolder: InstanceKlass by nonNull("_pool_holder")
    val operands: Array<Short>? by maybeNullArray("_operands")
    var cache: ConstantPoolCache? by maybeNull("_cache")
    val genericSignatureIndex: Short by nonNull("_generic_signature_index")
    val resolvedKlasses: Array<Klass> by nonNullArray("_resolved_klasses")
    val sourceFileNameIndex: Short by nonNull("_source_file_name_index")
    val refEntries = mutableListOf<Short>()
    val objectEntries = mutableListOf<Short>()

    val dataBase by lazy {
        address.base + vm.type("ConstantPool").size
    }

    val elementSize by lazy {
        vm.type("oop").size
    }

    fun getRefIndex(index: Short) = refEntries[index.toInt()]
    fun getObjectIndex(index: Short) = objectEntries[index.toInt()]

    val bytes by lazy {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        dos.writeShort(length)
        var index = 1
        while (index < length) {
            val tag = tags[index]!!.toInt()
            when (tag) {
                JVM_CONSTANT_Utf8 -> {
                    dos.writeByte(tag)
                    val symbol = getSymbol(index)
                    dos.writeShort(symbol.length)
                    dos.write(symbol.bytes)
                }

                JVM_CONSTANT_Integer -> {
                    dos.writeByte(tag)
                    dos.writeInt(getInt(index))
                }

                JVM_CONSTANT_Float -> {
                    dos.writeByte(tag)
                    dos.writeFloat(getFloat(index))
                }

                JVM_CONSTANT_Long -> {
                    dos.writeByte(tag)
                    dos.writeLong(getLong(index))
                    index++
                }

                JVM_CONSTANT_Double -> {
                    dos.writeByte(tag)
                    dos.writeDouble(getDouble(index))
                    index++
                }

                JVM_CONSTANT_Class -> {
                    dos.writeByte(tag)
                    val klass = getKlass(index)
                    val klassName = klass.name.string
                    dos.writeShort(getUtf8SymbolIndex(klassName))
                }

                JVM_CONSTANT_UnresolvedClass -> {
                    dos.writeByte(JVM_CONSTANT_Class)
                    dos.writeShort(getKlassNameIndex(index))
                }

                JVM_CONSTANT_UnresolvedClassInError -> {
                    dos.writeByte(JVM_CONSTANT_Class)
                    dos.writeShort(getKlassNameIndex(index))
                }

                JVM_CONSTANT_String -> {
                    dos.writeByte(tag)
                    val string = getString(index)
                    dos.writeShort(getUtf8SymbolIndex(string))
                }

                JVM_CONSTANT_Fieldref, JVM_CONSTANT_Methodref, JVM_CONSTANT_InterfaceMethodref, JVM_CONSTANT_NameAndType, JVM_CONSTANT_InvokeDynamic -> {
                    dos.writeByte(tag)
                    val refIndex = getInt(index)
                    dos.writeShort(refIndex.low.toInt())
                    dos.writeShort(refIndex.high.toInt())
                }

                JVM_CONSTANT_MethodHandle -> {
                    dos.writeByte(tag)
                    val value = getInt(index)
                    val refKind = value.low.toInt()
                    val refIndex = value.high.toInt()
                    dos.writeByte(refKind)
                    dos.writeShort(refIndex)
                }

                JVM_CONSTANT_MethodType -> {
                    dos.writeByte(tag)
                    val descriptorIndex = getInt(index)
                    dos.writeShort(descriptorIndex)
                }

                JVM_CONSTANT_ClassIndex -> {
                    dos.writeByte(JVM_CONSTANT_Class)
                    dos.writeShort(getInt(index))
                }

                JVM_CONSTANT_StringIndex -> {
                    dos.writeByte(JVM_CONSTANT_String)
                    dos.writeShort(getInt(index))
                }

                else -> error("Unknown constant pool tag: $tag (index: $index, length: $length)")
            }
            index++
        }
        bos.toByteArray()
    }

    private val _indy_bsm_offset = 0
    private val _indy_argc_offset = 1
    private val _indy_argv_offset = 2

    fun buildIntFromShorts(low: Short, high: Short): Int {
        return ((high.toInt() and 0xffff) shl 16) or (low.toInt() and 0xffff)
    }

    fun operandOffset(bootstrapSpecifierIndex: Int): Int {
        val n = bootstrapSpecifierIndex * 2

        require((n >= 0 && n + 2 <= operands!!.length)) { "BSI out of range (1): $bootstrapSpecifierIndex" }

        val secondPart: Int = buildIntFromShorts(
            operands!![n]!!,
            operands!![n + 1]!!
        )

        require((secondPart == 0 || n + 2 <= secondPart)) { "BSI out of range (2): $bootstrapSpecifierIndex" }

        val offset: Int = buildIntFromShorts(
            operands!![n]!!,
            operands!![n + 1]!!
        )

        require((offset == 0 || offset >= secondPart && offset <= operands!!.length)) { "BSI out of range (3): $bootstrapSpecifierIndex" }

        return offset
    }

    fun operandArrayLength(): Int {
        if (operands == null || operands!!.length == 0) {
            return 0
        }
        return (operandOffset(0) / 2)
    }

    fun operandArgumentCount(bootstrapSpecifierIndex: Int): Int {
        val offset: Int = operandOffset(bootstrapSpecifierIndex)
        return operands!![offset + _indy_argc_offset]!!.toInt()
    }

    fun operandArgumentIndex(bootstrapSpecifierIndex: Int, argumentIndex: Int): Int {
        val offset: Int = operandOffset(bootstrapSpecifierIndex)
        return operands!![offset + _indy_argv_offset + argumentIndex]!!.toInt()
    }

    fun operandNextOffset(bootstrapSpecifierIndex: Int): Int {
        val offset: Int = operandOffset(bootstrapSpecifierIndex) + _indy_argv_offset
        return offset + operandArgumentCount(bootstrapSpecifierIndex)
    }

    fun operandBootstrapMethodRefIndex(bootstrapSpecifierIndex: Int): Int {
        val offset: Int = operandOffset(bootstrapSpecifierIndex)
        return operands!![offset + _indy_bsm_offset]!!.toInt()
    }

    fun index(index: Int): Long {
        return dataBase + index.toLong() * elementSize
    }

    fun getSymbol(index: Int): Symbol {
        val address = unsafe.getAddress(index(index))
        return oops(address)!!
    }

    fun getString(index: Int): String {
        return getSymbol(index).string
    }

    fun getTag(index: Int) = tags[index]

    val Int.low
        get() =
            (toShort().toInt() and 0xffff).toShort()

    val Int.high
        get() =
            ((this shr 16).toShort().toInt() and 0xffff).toShort()

    data class CPKlassSlot(var nameIndex: Short, var resolvedKlassIndex: Short) {
        fun toInt(): Int {
            return ((nameIndex.toInt() and 0xffff) shl 16) or (resolvedKlassIndex.toInt() and 0xffff)
        }
    }

    fun getInt(index: Int): Int {
        return unsafe.getInt(index(index))
    }

    fun getLong(index: Int): Long {
        return unsafe.getLong(index(index))
    }

    fun getFloat(index: Int): Float {
        return unsafe.getFloat(index(index))
    }

    fun getDouble(index: Int): Double {
        return unsafe.getDouble(index(index))
    }


    private fun getKlassSlot(index: Int): CPKlassSlot {
        val value = getInt(index)
        return CPKlassSlot(value.high, value.low)
    }

    fun getKlass(index: Int): Klass {
        val resolved = getKlassSlot(index).resolvedKlassIndex
        val klassAddress = resolvedKlasses.getAddress(resolved.toInt())
        return Klass(Address(this, klassAddress))
    }

    fun getKlassNameAt(index: Int): Symbol {
        val slot = getKlassSlot(index)
        return getSymbol(slot.nameIndex.toInt())
    }

    fun getKlassNameIndex(index: Int): Int {
        val slot = getKlassSlot(index)
        return slot.nameIndex.toInt()
    }

    private var utf8SymbolMap = mutableMapOf<String, Int>()
    private var classSymbolMap = mutableMapOf<String, Int>()

    fun buildIndices() {
        var index = 1
        while (index < length) {
            val tag = tags[index]!!
            when (tag.toInt()) {
                JVM_CONSTANT_Utf8 -> {
                    val symbol = getSymbol(index)
                    utf8SymbolMap[symbol.string] = index
                }

                JVM_CONSTANT_Class,
                JVM_CONSTANT_UnresolvedClass,
                JVM_CONSTANT_UnresolvedClassInError -> {
                    val klassName = getKlassNameAt(index).string
                    classSymbolMap[klassName] = index
                }

                JVM_CONSTANT_String,
                JVM_CONSTANT_MethodType,
                JVM_CONSTANT_MethodHandle -> {
                    objectEntries += index.toShort()
                }

                JVM_CONSTANT_Fieldref,
                JVM_CONSTANT_Methodref,
                JVM_CONSTANT_InterfaceMethodref -> {
                    refEntries += index.toShort()
                }

                JVM_CONSTANT_Long,
                JVM_CONSTANT_Double -> {
                    index++
                }
            }
            index++
        }
    }

    fun getUtf8SymbolIndex(string: String): Int = utf8SymbolMap[string] ?: error("Symbol not present $string")
    fun getClassSymbolIndex(string: String): Int = classSymbolMap[string] ?: error("Class not present $string")

    data class ExpandInformation(val cacheMapping: Map<Int, Int>, val referenceMapping: Map<Int, Int>, val pool: ConstantPool)

    fun expand(entries: MutableList<CpEntry>): ExpandInformation {
        if (entries.isEmpty()) return ExpandInformation(emptyMap(), emptyMap(), this)
        if (entries.size + length > 0xffff) error("Constant pool size limit exceeded")

        // simple optimization, remove strings already in the pool
        entries.filterIsInstance<CpUtf8>().forEach {
            if (utf8SymbolMap.containsKey(it.text)) {
                it.index = utf8SymbolMap[it.text]!!
            }
        }
        entries.removeIf { it is CpUtf8 && utf8SymbolMap.containsKey(it.text) }

        // expand the constant pool
        val oldSize = structs.sizeof(ConstantPool::class) + (length * elementSize)
        val newSize = oldSize + (entries.size * elementSize)
        val newPoolAddress = unsafe.allocateMemory(newSize.toLong())
        unsafe.copyMemory(address.base, newPoolAddress, oldSize)

        val newPool: ConstantPool = oops(newPoolAddress)!!

        // create new tags array
        val expandedTags = tags.expand(entries.size)
        newPool.tags = expandedTags
        for (tag in newPool.tags) {
            println("tag: $tag")
        }

        newPool.length = length + entries.size

        // expand entries list to account for long and double entries

        val cacheEntries = mutableListOf<ConstantPoolCacheEntry>()
        val cacheMapping = mutableMapOf<Int, Int>()
        val referenceMapping = mutableMapOf<Int, Int>()
        val referenceMap = mutableListOf<Short>()

        var cacheSize = cache!!.length
        var refsSize = cache!!.referenceMap!!.length

        fun allocateEntry(tag: Int, index: Int): Int {
            val entryAddress = unsafe.allocateMemory(structs.sizeof(ConstantPoolCacheEntry::class).toLong())
            val entry: ConstantPoolCacheEntry = structs(entryAddress)!!
            entry.f1 = 0
            entry.f2 = 0
            entry.flags = 0
            cacheEntries.add(entry)

            entry.setCpIndex(index)

            return cacheSize + cacheEntries.size - 1
        }

        fun allocateRefEntry(index: Int): Int {
            referenceMap.add(index.toShort())

            return refsSize + referenceMap.size - 1
        }

        val expanded = entries.map { entry ->
            when (entry) {
                is CpLong, is CpDouble -> {
                    listOf(entry, CpInt(0))
                }

                else -> {
                    listOf(entry)
                }
            }
        }.flatten()

        // update internal references
        expanded.forEachIndexed { index, entry -> entry.index = length + index }

        val symbolMap = mutableMapOf<Int, Symbol>()

        expanded.forEachIndexed { index, entry ->
            val newIndex = length + index
            expandedTags[newIndex] = entry.tag.toByte()
            when (entry) {
                is CpUtf8 -> {
                    val symbol = Symbol.create(entry.text, this)
                    symbolMap[newIndex] = symbol
                    newPool[newIndex] = symbol.address
                }

                is CpClass -> {
                    expandedTags[newIndex] = JVM_CONSTANT_UnresolvedClass.toByte()
                    // high part is the name index, low part is the resolved klass index

                    val klassSlot = CPKlassSlot(entry.name.index.low, 0)
                    newPool[newIndex] = klassSlot.toInt()
                }

                is CpDouble -> {
                    newPool[newIndex] = entry.value
                }

                is CpLong -> {
                    newPool[newIndex] = entry.value
                }

                is CpInt -> {
                    newPool[newIndex] = entry.value
                }

                is CpFloat -> {
                    newPool[newIndex] = entry.value.toRawBits()
                }

                is CpString -> {
                    val symbol = symbolMap[entry.string.index] ?: error("Symbol not found")
                    val stringIndex = allocateRefEntry(entry.index)

                    referenceMapping[newIndex] = stringIndex

                    newPool[newIndex] = symbol.address
                }

                is CpNameType -> {
                    newPool[newIndex] = (entry.type.index shl 16) or entry.name.index
                }

                is ConstRef -> {
                    newPool[newIndex] = (entry.nameType.index shl 16) or entry.classRef.index
                    // build constant pool entry
                    val cacheIndex = allocateEntry(entry.tag, newIndex)

                    cacheMapping[newIndex] = cacheIndex
                }

                is CpInvokeDynamic, is CpDynamic -> {
                    TODO("fill in operands also do some other stuff, havent researched, look at cpCache indy objects")
                }

                is CpPackage, is CpModule -> {
                    TODO("no one cares")
                }

                else -> error("Unsupported entry type: ${entry::class}")
            }
        }

        // TODO: expand reference map and resolved entries map in cp cache

        val newCache = cache!!.expand(cacheEntries, referenceMap)
        newPool.cache = newCache

        return ExpandInformation(cacheMapping, referenceMapping, newPool)
    }

    private inline operator fun <reified T> set(index: Int, value: T) {
        val offset = index(index)
        when (T::class) {
            Byte::class -> unsafe.putByte(offset, value as Byte)
            Short::class -> unsafe.putShort(offset, value as Short)
            Int::class -> unsafe.putInt(offset, value as Int)
            Long::class -> unsafe.putLong(offset, value as Long)
            Float::class -> unsafe.putFloat(offset, value as Float)
            Double::class -> unsafe.putDouble(offset, value as Double)
            Address::class -> unsafe.putAddress(offset, (value as Address).base)
            else -> error("Unsupported type: ${T::class}")
        }
    }
}