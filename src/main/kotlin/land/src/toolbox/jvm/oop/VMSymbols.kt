package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.offset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class VMSymbols(address: Address): Struct(address) {
    private val symbols: Long by offset("_vm_symbols[0]")

    fun lookupSymbol(index: Int): Symbol {
        val symbolAddress = unsafe.getAddress(symbols + index * type.size)
        return oops(symbolAddress)!!
    }

    override val typeName = "Symbol"
}