package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address

class nmethod(address: Address) : CompiledMethod(address) {

    val entryPoint: Long by nonNull("_entry_point")
    val verifiedEntryPoint: Long by nonNull("_verified_entry_point")
    val osrLink: nmethod? by maybeNull("_osr_link")

    override val typeName = "nmethod"

}