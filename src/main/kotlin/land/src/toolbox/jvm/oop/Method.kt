package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.util.*
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct

class Method(address: Address) : Struct(address), Oop {
    val maxStack: Short get() = constMethod.maxStack
    val maxLocals: Short get() = constMethod.maxLocals
    val accessFlags: Short by nonNull("_access_flags")
    val constMethod: ConstMethod by nonNull("_constMethod")
    val fromInterpretedEntry: Long by nonNull("_from_interpreted_entry")
    val fromCompiledEntry: Long by nonNull("_from_compiled_entry")

    val compiledMethod: CompiledMethod? by maybeNull("_code")

    val isNative: Boolean get() = accessFlags.toInt() and JVM_ACC_NATIVE != 0
}