package land.src.toolbox.jvm

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class VMArguments(address: Address) : Struct(address) {
    val vmFlagsCount: Int by nonNull("_num_jvm_flags")
    val vmFlagsStart: Long by nonNull("_num_jvm_flags")
    val vmArgsCount: Int by nonNull("_num_jvm_flags")
    val vmArgsStart: Long by nonNull("_jvm_args_array")
    val javaCommand: String by nonNull("_java_command", true)

    fun array(count: Int, start: Long): List<String> {
        val value = mutableListOf<String>()

        val stride = if (vm.is64Bit) 8 else 4
        for (i in 0 until count) {
            val stringAddress = unsafe.getAddress(start + (i * stride))
            value += unsafe.getString(stringAddress)!!
        }

        return value
    }

    val args by lazy { array(vmArgsCount, vmArgsStart) }
    val flags by lazy { array(vmFlagsCount, vmFlagsStart) }

    override fun toString() =
        args.joinToString("\n") + flags.joinToString("\n") + javaCommand

    override val typeName = "Arguments"
}