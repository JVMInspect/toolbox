package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.short
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class LocalVariableTableElement(address: Address) : Struct(address) {
    val length by short("length")
    val nameCpIndex by short("name_cp_index")
    val descriptorCpIndex by short("descriptor_cp_index")
    val signatureCpIndex by short("signature_cp_index")
    val slot by short("slot")
    val startBci by short("start_bci")
}