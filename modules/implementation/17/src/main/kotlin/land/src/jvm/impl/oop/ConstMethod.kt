package land.src.jvm.impl.oop

import land.src.jvm.impl.oop.pool.ConstantPool
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.ConstMethod as ConstMethodApi

class ConstMethod(address: Address) : Struct(address), ConstMethodApi {
    override val maxStack: Short by nonNull("_max_stack")
    override val maxLocals: Short by nonNull("_max_locals")
    override val constants: ConstantPool by nonNull("_constants")

    val id: Short by nonNull("_method_idnum")
    val nameIndex: Short by nonNull("_name_index")
    val signatureIndex: Short by nonNull("_signature_index")

    val codeSize: Short by nonNull("_code_size")
    val constMethodSize: Int by nonNull("_constMethod_size")

    val codeOffset: Long get() = vmType.size.toLong()
    val codeEndOffset: Long get() = codeOffset + codeSize

    val flags: Int by nonNull("_flags")

    val hasLineNumberTable: Boolean get() = flags and (1 shl 0) != 0
    val hasCheckedExceptions: Boolean get() = flags and (1 shl 1) != 0
    val hasLocalVariableTable: Boolean get() = flags and (1 shl 2) != 0
    val hasExceptionTable: Boolean get() = flags and (1 shl 3) != 0
    val hasGenericSignature: Boolean get() = flags and (1 shl 4) != 0
    val hasMethodParameters: Boolean get() = flags and (1 shl 5) != 0
    val isOverpass: Boolean get() = flags and (1 shl 6) != 0
    val hasMethodAnnotations: Boolean get() = flags and (1 shl 7) != 0
    val hasParameterAnnotations: Boolean get() = flags and (1 shl 8) != 0
    val hasTypeAnnotations: Boolean get() = flags and (1 shl 9) != 0
    val hasDefaultAnnotations: Boolean get() = flags and (1 shl 10) != 0


}