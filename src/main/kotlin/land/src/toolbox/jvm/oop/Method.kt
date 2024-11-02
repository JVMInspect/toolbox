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
    var fromInterpretedEntry: Long by nonNull("_from_interpreted_entry")
    var fromCompiledEntry: Long by nonNull("_from_compiled_entry")
    val i2iEntry: Long by nonNull("_i2i_entry")

    val adapter: AdapterHandlerEntry? by maybeNull {
        val adapterOffset = type.field("_method_counters")!!.offsetOrAddress + pointerSize
        offset(adapterOffset)
    }

    var compiledMethod: CompiledMethod? get() {
        val address: Long = unsafe.getAddress(address.base + type.field("_code")!!.offsetOrAddress)
        if (address == 0L) return null
        return CompiledMethod(Address(this, address))
    }
    set(value) {
        unsafe.putAddress(address.base + type.field("_code")!!.offsetOrAddress, value?.base ?: 0)
    }

    val isNative: Boolean get() = accessFlags.toInt() and JVM_ACC_NATIVE != 0

    fun deoptimizie() {
        if (compiledMethod == null) return // not optimized

        val nmethod = compiledMethod!!.native

        nmethod.makeNotEntrant()

        clearCode()
    }

    fun clearCode() {
        fromCompiledEntry = if (adapter == null) {
            0
        } else {
            adapter!!.c2iEntry
        }
        fromInterpretedEntry = i2iEntry
        compiledMethod = null
    }
}