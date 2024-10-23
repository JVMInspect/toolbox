package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class ConstantPoolCache(address: Address) : Struct(address) {
    val constantPool: ConstantPool by nonNull("_constant_pool")
    val referenceMap: Array<Short> by nonNull("_reference_map")
    val resolvedFieldEntries: Array<ResolvedFieldEntry> by nonNull("_resolved_field_entries")
    val resolvedIndyEntries: Array<ResolvedIndyEntry> by nonNull("_resolved_indy_entries")
    val resolvedMethodEntries: Array<ResolvedMethodEntry> by nonNull("_resolved_method_entries")
    //val resolvedReferences: OopHandle by struct("_resolved_references")


}