package land.src.jvmtb.jvm.oop

import land.src.jvmtb.util.ClassConstants.JVM_CONSTANT_Class
import land.src.jvmtb.util.ClassConstants.JVM_CONSTANT_Utf8
import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.maybeNullArray
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.nonNullArray
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Struct
import land.src.toolbox.jvm.primitive.Address

class ConstantPool(address: Address) : Struct(address) {
    val length: Int by nonNull("_length")
    val majorVersion: Short by nonNull("_major_version")
    val minorVersion: Short by nonNull("_minor_version")
    val tags: Array<Byte> by nonNullArray("_tags")
    val operands: Array<Short>? by maybeNullArray("_operands")
    val genericSignatureIndex: Short by nonNull("_generic_signature_index")
    val resolvedKlasses: Array<Klass> by nonNullArray("_resolved_klasses")
    val sourceFileNameIndex: Short by nonNull("_source_file_name_index")

    val dataBase by lazy {
        address.base + vm.type("ConstantPool").size
    }

    val elementSize by lazy {
        vm.type("oop").size
    }

    val bytes by lazy {
        unsafe.getMemory(dataBase, length * elementSize)
    }

    fun index(index: Int): Long {
        return dataBase + index.toLong() * elementSize
    }

    fun getSymbol(index: Int): Symbol {
        val address = unsafe.getAddress(index(index))
        return Symbol(Address(this, address))
    }

    val Int.low get() =
        (this.toShort().toInt() and 0xffff).toShort()

    val Int.high get() =
            ((this shr 16).toShort().toInt() and 0xffff).toShort()

    class CPKlassSlot(var nameIndex: Short, var resolvedKlassIndex: Short)

    fun getInt(index: Int): Int {
        return unsafe.getInt(index(index))
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

    private var utf8SymbolMap = mutableMapOf<String, Int>()
    private var classSymbolMap = mutableMapOf<String, Int>()

    fun buildIndices() {
        for (index in 1 until length) {
            val tag = tags[index]!!
            when (tag.toInt()) {
                JVM_CONSTANT_Utf8 -> {
                    val symbol = getSymbol(index)
                    utf8SymbolMap[symbol.string] = index
                }
                JVM_CONSTANT_Class -> {
                    val klass = getKlass(index)
                    val klassName = klass.name.string
                    classSymbolMap[klassName] = index
                }
            }
        }
    }

    fun getUtf8SymbolIndex(string: String): Int = utf8SymbolMap[string] ?: error("Symbol not present $string")
    fun getClassSymbolIndex(string: String): Int = classSymbolMap[string] ?: error("Class not present $string")
}