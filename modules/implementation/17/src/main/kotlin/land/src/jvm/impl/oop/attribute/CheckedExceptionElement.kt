package land.src.jvm.impl.oop.attribute

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.attribute.CheckedExceptionElement as CheckedExceptionElementApi

class CheckedExceptionElement(address: Address) : Struct(address), CheckedExceptionElementApi {
    override val classIndex: Short by nonNull("class_cp_index")
}