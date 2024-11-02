package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class AdapterHandlerEntry(address: Address) : Struct(address) {

    private val baseOffset: Int by lazy { vm.type("BasicHashtableEntry<mtInternal>").size }

    val i2cEntry: Long by nonNull {
        offset(baseOffset + pointerSize)
    }

    val c2iEntry: Long by nonNull {
        offset(baseOffset + pointerSize * 2)
    }

}