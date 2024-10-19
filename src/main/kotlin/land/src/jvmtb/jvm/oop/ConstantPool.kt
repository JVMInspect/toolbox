package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.int
import land.src.jvmtb.dsl.nullableArray
import land.src.jvmtb.dsl.short
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class ConstantPool(address: Address) : Struct(address) {
    val length: Int by int("_length")
    val majorVersion: Short by short("_major_version")
    val minorVersion: Short by short("_minor_version")
    val operands: Array<Short>? by nullableArray("_operands")
    val genericSignatureIndex: Short by short("_generic_signature_index")
}