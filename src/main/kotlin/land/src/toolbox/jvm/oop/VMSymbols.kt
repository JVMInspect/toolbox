package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class VMSymbols(address: Address): Struct(address) {

    private val _vm_symbols: Long by lazy { type.field("_vm_symbols[0]")!!.offsetOrAddress }

    fun lookupSymbol(index: Int): Symbol {
        val symbolAddress = unsafe.getAddress(_vm_symbols + index * type.size)
        return oops(symbolAddress)!!
    }

    override val typeName = "Symbol"
}