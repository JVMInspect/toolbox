package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.array
import land.src.jvmtb.dsl.nullableArray
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class Annotations(address: Address) : Struct(address) {
    val classAnnotations: Array<Byte>? by nullableArray("_class_annotations")
    val classTypeAnnotations: Array<Byte>? by nullableArray("_class_type_annotations")
    val fieldsAnnotations: Array<Array<Byte>>? get() {
        TODO("Not yet implemented")
        //val fieldAddr = address.base + type.field("_fields_annotations").offsetOrAddress
    }//by nullableArray("_fields_annotations")
    val fieldsTypeAnnotations: Array<Array<Byte>>? by nullableArray("_fields_type_annotations")
}