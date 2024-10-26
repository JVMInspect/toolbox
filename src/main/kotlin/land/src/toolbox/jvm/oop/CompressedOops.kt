package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class CompressedOops(address: Address) : Struct(address) {
    val narrowOopBase: Long by nonNull("_narrow_oop._base")
    val narrowOopShift: Int by nonNull("_narrow_oop._shift")
}