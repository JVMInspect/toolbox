package land.src.jvm.api.oop.pool

import land.src.jvm.api.NPrimitiveArray

interface ConstantPool {
    val length: Int
    val major: Short
    val minor: Short
    val cache: ConstantPoolCache?
    val tags: NPrimitiveArray<Byte>
    val operands: NPrimitiveArray<Short>?
}