package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class Method(address: Address) : Struct(address) {
    val annotations: Array<Annotation>? get() = TODO()
    val parameterAnnotations: Array<Annotation>? get() = TODO()
    val defaultAnnotations: Array<Annotation>? get() = TODO()
    val typeAnnotations: Array<Annotation>? get() = TODO()
    val maxStack: Int get() = TODO()
    val maxLocals: Int get() = TODO()
    val accessFlags: AccessFlags get() = TODO()
    val constMethod: ConstMethod get() = TODO()

    val isOverpass: Boolean get() = TODO()
}