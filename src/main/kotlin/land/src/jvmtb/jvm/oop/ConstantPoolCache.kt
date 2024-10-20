package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.array
import land.src.jvmtb.dsl.struct
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class ConstantPoolCache(address: Address) : Struct(address) {
    val constantPool: ConstantPool by struct("_constant_pool")
    val referenceMap: Array<Short> by array("_reference_map")
    val resolvedFieldEntries: Array<ResolvedFieldEntry> by array("_resolved_field_entries")
    val resolvedIndyEntries: Array<ResolvedIndyEntry> by array("_resolved_indy_entries")
    val resolvedMethodEntries: Array<ResolvedMethodEntry> by array("_resolved_method_entries")
    //val resolvedReferences: OopHandle by struct("_resolved_references")
}