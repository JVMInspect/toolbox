package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.util.*
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import java.util.concurrent.locks.LockSupport

class Method(address: Address) : Struct(address), Oop {
    val maxStack: Short get() = constMethod.maxStack
    val maxLocals: Short get() = constMethod.maxLocals
    val accessFlags: Short by nonNull("_access_flags")
    val constMethod: ConstMethod by nonNull("_constMethod")
    var fromInterpretedEntry: Long by nonNull("_from_interpreted_entry")
    var fromCompiledEntry: Long by nonNull("_from_compiled_entry")
    val i2iEntry: Long by nonNull("_i2i_entry")

    val adapter: AdapterHandlerEntry? by maybeNull {
        val adapterOffset = type.field("_method_counters")!!.offsetOrAddress + pointerSize
        offset(adapterOffset)
    }

    var compiledMethod: CompiledMethod? by maybeNull("_code")

    val isNative: Boolean get() = accessFlags.toInt() and JVM_ACC_NATIVE != 0

    fun deoptimizie() {
        if (compiledMethod == null) return // not optimized

        val nmethod = compiledMethod!!.native

        nmethod.lockCount = 1
        nmethod.makeNotEntrant()

        clearCode()
        nmethod.lockCount = 0
    }

    fun clearCode() {
        fromCompiledEntry = adapter?.c2iEntry ?: 0
        fromInterpretedEntry = i2iEntry
        compiledMethod = null
    }
}