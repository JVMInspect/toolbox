package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class AccessFlags(address: Address) : Struct(address) {
    val flags: Int get() = TODO()
    val isSynthetic: Boolean get() = TODO()
}