package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address

class NMethod(address: Address) : CompiledMethod(address) {

    val entryPoint: Long by nonNull("_entry_point")
    val verifiedEntryPoint: Long by nonNull("_verified_entry_point")
    val osrLink: NMethod? by maybeNull("_osr_link")

}