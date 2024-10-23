package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class ExceptionTableElement(address: Address) : Struct(address) {
    val catchTypeIndex: Short by nonNull("catch_type_index")
    val endPc: Short by nonNull("end_pc")
    val handlerPc: Short by nonNull("handler_pc")
    val startPc: Short by nonNull("start_pc")
}