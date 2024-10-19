package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.short
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class ExceptionTableElement(address: Address) : Struct(address) {
    val catchTypeIndex by short("catch_type_index")
    val endPc by short("end_pc")
    val handlerPc by short("handler_pc")
    val startPc by short("start_pc")
}