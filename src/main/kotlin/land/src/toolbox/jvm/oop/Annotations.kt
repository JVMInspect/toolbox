package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.maybeNullArray
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.ByteArray
import land.src.toolbox.jvm.primitive.Struct

class Annotations(address: Address) : Struct(address) {
    val classAnnotations: Array<Byte>? by maybeNullArray {
        address(address.base)
    }
    val fieldsAnnotations: Array<ByteArray>? by maybeNullArray {
        address(address.base + pointerSize)
    }
    val classTypeAnnotations: Array<Byte>? by maybeNullArray {
        address(address.base + pointerSize * 2)
    }
    val fieldsTypeAnnotations: Array<ByteArray>? by maybeNullArray {
        address(address.base + pointerSize * 3)
    }
}