package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.short
import land.src.jvmtb.dsl.struct
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class Method(address: Address) : Struct(address) {
    val maxStack: Short get() = constMethod.maxStack
    val maxLocals: Short get() = constMethod.maxLocals
    val accessFlags: Short by short("_access_flags")
    val constMethod: ConstMethod by struct("_constMethod")
}