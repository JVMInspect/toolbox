package land.src.jvm.impl.oop.attribute

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.attribute.ExceptionTableElement as ExceptionTableElementApi

class ExceptionTableElement(address: Address) : Struct(address), ExceptionTableElementApi {
    override val end: Short by nonNull("end_pc")
    override val start: Short by nonNull("start_pc")
    override val handler: Short by nonNull("handler_pc")
    override val typeIndex: Short by nonNull("catch_type_index")
}