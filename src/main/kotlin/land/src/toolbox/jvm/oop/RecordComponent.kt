package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.toolbox.jvm.primitive.Array

class RecordComponent(address: Address) : Struct(address) {
    val annotations: Array<Byte>? by lazy {
        arrays(address.base, false)
    }

    val typeAnnotations: Array<Byte>? by lazy {
        arrays(address.base + pointerSize, false)
    }

    // todo: accounts for _attributes_count in JDK 17
    val genericSignatureIndexOffset by lazy {
        (pointerSize * 2) + (Short.SIZE_BYTES * 3)
    }

    val nameIndex: Short by lazy {
        unsafe.getShort(address.base + (pointerSize * 2))
    }

    val descriptorIndex: Short by lazy {
        unsafe.getShort(address.base + (pointerSize * 2) + Short.SIZE_BYTES)
    }

    val attributesCount: Short by lazy {
        unsafe.getShort(address.base + genericSignatureIndexOffset - Short.SIZE_BYTES)
    }

    val genericSignatureIndex: Short by lazy {
        unsafe.getShort(address.base + genericSignatureIndexOffset)
    }
}