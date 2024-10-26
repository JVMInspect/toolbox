package land.src.toolbox.jvm

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class VMFlags(address: Address) : Struct(address) {
    data class Flag(val name: String, val type: Int, val value: Int)

    val vmFlagsCount: Int by nonNull("numFlags")
    val vmFlagsStart: Long by nonNull("flags")
    val flagName: String by nonNull("_name", true)
    val flagType: Int by nonNull("_type")
    val flagValue: Int by nonNull("_flags")

    val flags by lazy {
        val value = mutableListOf<Flag>()
        for (i in 0 until vmFlagsCount - 1) {
            val flagAddress = vmFlagsStart + (i * type.size)
            val flag = structs<VMFlags>(flagAddress)!!
            value += Flag(flag.flagName, flag.flagType, flag.flagValue)
        }
        value
    }

    override val typeName: String = "JVMFlag"
}