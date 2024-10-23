package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class LocalVariableTableElement(address: Address) : Struct(address) {
    val startBci: Short by nonNull("start_bci")
    val length: Short by nonNull("length")
    val nameCpIndex: Short by nonNull("name_cp_index")
    val descriptorCpIndex: Short by nonNull("descriptor_cp_index")
    val signatureCpIndex: Short by nonNull("signature_cp_index")
    val slot: Short by nonNull("slot")
}