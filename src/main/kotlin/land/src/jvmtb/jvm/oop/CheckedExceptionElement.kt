package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.short
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class CheckedExceptionElement(address: Address) : Struct(address) {
    val classCpIndex by short("class_cp_index")
}