package land.src.jvmtb.jvm.oop

import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.ByteArray
import land.src.toolbox.jvm.primitive.Struct

class Annotations(address: Address) : Struct(address) {
    val classAnnotations: Array<Byte>? by lazy {
        val classAnnotationsArrayAddress = unsafe.getAddress(address.base)
        arrays<Byte, Array<Byte>>(classAnnotationsArrayAddress, false)
    }
    val fieldsAnnotations: Array<ByteArray>? by lazy {
        val fieldsAnnotationsArrayAddress = unsafe.getAddress(address.base + (pointerSize))
        arrays(fieldsAnnotationsArrayAddress, false)
    }
    val classTypeAnnotations: Array<Byte>? by lazy {
        val classTypeAnnotationsArrayAddress = unsafe.getAddress(address.base + (pointerSize * 2))
        arrays<Byte, Array<Byte>>(classTypeAnnotationsArrayAddress, false)
    }
    val fieldsTypeAnnotations: Array<ByteArray>? by lazy {
        val fieldsTypeAnnotationsArrayAddress = unsafe.getAddress(address.base + (pointerSize * 3))
        arrays(fieldsTypeAnnotationsArrayAddress, false)
    }
}