package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class AccessFlags(address: Address) : Struct(address) {
    val flags: Int get() = TODO()
    val isSynthetic: Boolean get() = TODO()
}