package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class CheckedExceptionElement(address: Address) : Struct(address) {
    val classCpIndex: Short by nonNull("class_cp_index")
}