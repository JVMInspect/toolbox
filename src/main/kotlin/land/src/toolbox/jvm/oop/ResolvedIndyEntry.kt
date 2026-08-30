package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

/**
 * Where JDK 21 keeps the resolution state of one `invokedynamic` call site.
 *
 * Before JDK 21 an indy shared the ConstantPoolCacheEntry table with field and method accesses, and
 * the bytecode operand indexed into that; from JDK 21 on it indexes into `_resolved_indy_entries`
 * instead, which is why the operand has to be resolved through here.
 */
class ResolvedIndyEntry(address: Address) : Struct(address) {
    val cpIndex: Short by nonNull("_cpool_index")
}
