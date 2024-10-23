package land.src.jvmtb.jvm.oop

import land.src.jvmtb.util.ClassConstants.JVM_ACC_NATIVE
import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class Method(address: Address) : Struct(address) {
    val maxStack: Short get() = constMethod.maxStack
    val maxLocals: Short get() = constMethod.maxLocals
    val accessFlags: Short by nonNull("_access_flags")
    val constMethod: ConstMethod by nonNull("_constMethod")

    val isNative: Boolean get() = accessFlags.toInt() and JVM_ACC_NATIVE != 0
}