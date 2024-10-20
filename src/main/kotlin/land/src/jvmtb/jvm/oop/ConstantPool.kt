package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.array
import land.src.jvmtb.dsl.int
import land.src.jvmtb.dsl.nullableArray
import land.src.jvmtb.dsl.short
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.util.ClassConstants.JVM_CONSTANT_Class
import land.src.jvmtb.util.ClassConstants.JVM_CONSTANT_Utf8

class ConstantPool(address: Address) : Struct(address) {
    val length: Int by int("_length")
    val majorVersion: Short by short("_major_version")
    val minorVersion: Short by short("_minor_version")
    val tags: Array<Byte> by array("_tags")
    val operands: Array<Short>? by nullableArray("_operands")
    val genericSignatureIndex: Short by short("_generic_signature_index")
    val resolvedKlasses: Array<Klass> by array("_resolved_klasses")

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
            val tag = tags[index]
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
    fun getClassSymbolIndex(string: String): Int = classSymbolMap[string] ?: error("ClassS not present $string")
}