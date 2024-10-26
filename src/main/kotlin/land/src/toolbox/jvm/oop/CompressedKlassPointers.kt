package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class CompressedKlassPointers(address: Address) : Struct(address) {
    val narrowKlassBase: Long by nonNull("_narrow_klass._base")
    val narrowKlassShift: Int by nonNull("_narrow_klass._shift ")
}