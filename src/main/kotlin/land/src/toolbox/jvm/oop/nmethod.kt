package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address

val not_installed: Byte = -1
val in_use: Byte = 0
val not_used: Byte = 1
val not_entrant: Byte = 2
val unloaded: Byte = 3
val zombie: Byte = 4

class NMethod(address: Address) : CompiledMethod(address) {
    val entryPoint: Long by nonNull("_entry_point")
    val entryPointBci: Int by nonNull("_entry_bci")
    val verifiedEntryPoint: Long by nonNull("_verified_entry_point")
    var osrLink: NMethod? by maybeNull("_osr_link")
    var state: Byte by nonNull("_state")

    fun isOsrMethod() = entryPointBci != -1
    fun isInUse() = state <= in_use
    fun isNotEntrant() = state == not_entrant

    fun makeNotEntrant() {
        if (isOsrMethod() && isInUse()) { // osr method
            // invalid osr entry point
            if (method != null) {
                method!!.constMethod.constants.poolHolder.removeOsrNMethod(this)
            }
        }

        if (!isOsrMethod() && !isNotEntrant()) {
            // patch verified entry point

        }

        state = not_entrant
    }

    override val typeName = "nmethod"
}