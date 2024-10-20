package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address

class Field(
    val accessFlags: Short,
    val nameIndex: Short,
    val signatureIndex: Short,
    val initialValueIndex: Short,
    val lowOffset: Short,
    val highOffset: Short
) {
    constructor(address: Address) : this(
        address.scope.unsafe.getShort(address.base + 0 * Short.SIZE_BYTES),
        address.scope.unsafe.getShort(address.base + 1 * Short.SIZE_BYTES),
        address.scope.unsafe.getShort(address.base + 2 * Short.SIZE_BYTES),
        address.scope.unsafe.getShort(address.base + 3 * Short.SIZE_BYTES),
        address.scope.unsafe.getShort(address.base + 4 * Short.SIZE_BYTES),
        address.scope.unsafe.getShort(address.base + 5 * Short.SIZE_BYTES),
    )
}