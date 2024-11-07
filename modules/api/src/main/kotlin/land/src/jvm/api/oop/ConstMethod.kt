package land.src.jvm.api.oop

import land.src.jvm.api.oop.pool.ConstantPool

interface ConstMethod {
    val maxStack: Short
    val maxLocals: Short
    val constants: ConstantPool
}