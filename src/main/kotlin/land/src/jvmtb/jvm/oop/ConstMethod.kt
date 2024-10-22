package land.src.jvmtb.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.maybeNullArray
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.toolbox.jvm.primitive.Array

class ConstMethod(address: Address) : Struct(address) {
    val maxStack: Short by nonNull("_max_stack")
    val maxLocals: Short by nonNull("_max_locals")

    val stackMapData: Array<Byte>? by maybeNullArray("_stackmap_data")
    val hasStackMapTable: Boolean get() = stackMapData != null

    val constants: ConstantPool by nonNull("_constants")

    val nameIndex: Short by nonNull("_name_index")
    val signatureIndex: Short by nonNull("_signature_index")

    val codeSize: Short by nonNull("_code_size")
    val constMethodSize: Int by nonNull("_constMethod_size")

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

    val methodAnnotationsAddr: Long get() {
        return constMethodEnd - pointerSize
    }

    val methodAnnotations: Array<Byte>? get() {
        if (!hasMethodAnnotations)
            return null

        return arrays(methodAnnotationsAddr, false)
    }

    val parameterAnnotationsAddr: Long get() {
        var offset = 1
        if (hasMethodAnnotations) offset++
        return constMethodEnd - (offset * pointerSize)
    }

    val parameterAnnotations: Array<Byte>? get() {
        if (!hasParameterAnnotations)
            return null
        return arrays(parameterAnnotationsAddr, false)
    }

    val typeAnnotationsAddr: Long get() {
        var offset = 1
        if (hasMethodAnnotations) offset++
        if (hasParameterAnnotations) offset++
        return constMethodEnd - (offset * pointerSize)
    }

    val typeAnnotations: Array<Byte>? get() {
        if (!hasTypeAnnotations)
            return null
        return arrays(typeAnnotationsAddr, false)
    }

    val defaultAnnotationsAddr: Long get() {
        var offset = 1
        if (hasMethodParameters) offset++
        if (hasParameterAnnotations) offset++
        if (hasTypeAnnotations) offset++
        return constMethodEnd - (offset * pointerSize)
    }

    val defaultAnnotations: Array<Byte>? get() {
        if (!hasDefaultAnnotations)
            return null
        return arrays(defaultAnnotationsAddr, false)
    }

    val lineNumberTableEntries: Int get() = TODO()

    val constMethodEnd get(): Long {
        return address.base + (constMethodSize * pointerSize)
    }

    val codeBase: Long get() = address.base + pointerSize
    val codeEnd: Long get() = codeBase + codeSize

    val compressedLineNumberTable: Array<Short> get() {
        return arrays(address = address.base + codeEnd, isElementPointer = false)!!
    }

    /**
     *     int offset = 0;
     *     if (hasMethodAnnotations()) offset++;
     *     if (hasParameterAnnotations()) offset++;
     *     if (hasTypeAnnotations()) offset++;
     *     if (hasDefaultAnnotations()) offset++;
     *     long wordSize = VM.getVM().getObjectHeap().getOopSize();
     *     return (getSize() * wordSize) - (offset * wordSize) - sizeofShort;
     */

    val lastU2Element: Long get() {
        var offset = 0
        if (hasMethodAnnotations) offset++
        if (hasParameterAnnotations) offset++
        if (hasTypeAnnotations) offset++
        if (hasDefaultAnnotations) offset++
        return constMethodEnd - (offset * pointerSize) - Short.SIZE_BYTES
    }

    val genericSignatureIndexAddress: Long get() =
        lastU2Element

    val genericSignatureIndex: Short get() =
        if (hasGenericSignature) unsafe.getShort(genericSignatureIndexAddress) else 0

    val methodParametersLengthAddr: Long get() =
        if (hasGenericSignature) lastU2Element - Short.SIZE_BYTES else lastU2Element

    val methodParametersLength: Short get() =
        if (hasMethodParameters) unsafe.getShort(methodParametersLengthAddr) else 0

    val methodParametersStart: Long get() {
        val addr = methodParametersLengthAddr
        val length = unsafe.getLong(addr)
        return addr - length * /* sizeof(MethodParametersElement) */ 4 /// /* sizeof(u2) */ 2
    }

    val checkedExceptionsStart: Long get() {
        val addr = checkedExceptionsLengthAddr
        val length = unsafe.getLong(addr)
        return addr - length * /* sizeof(CheckedExceptionElement) */ 2 /// /* sizeof(u2) */ 2
    }

    val checkedExceptionsLength: Short get() =
        if (hasCheckedExceptions) unsafe.getShort(checkedExceptionsLengthAddr) else 0

    val exceptionTableStart: Long get() {
        val addr = exceptionTableLengthAddr
        val length = unsafe.getLong(addr)
        return addr - length * /* sizeof(ExceptionTableElement) */ 8 /// /* sizeof(u2) */ 2
    }

    val localVariableTableLengthAddr: Long get() =
        if (hasExceptionTable) exceptionTableStart - Short.SIZE_BYTES
        else if (hasCheckedExceptions) checkedExceptionsStart - Short.SIZE_BYTES
        else if (hasMethodParameters) methodParametersStart - Short.SIZE_BYTES
        else if (hasGenericSignature) lastU2Element - Short.SIZE_BYTES
        else lastU2Element

    val localVariableTableLength: Short get() =
        if (hasLocalVariableTable) unsafe.getShort(localVariableTableLengthAddr) else 0

    val localVariableTableStart: Long get() {
        val addr = localVariableTableLengthAddr
        val length = unsafe.getLong(addr)
        return addr - length * /* sizeof(LocalVariableTableElement) */ 12 /// /* sizeof(u2) */ 2
    }

    val exceptionTableLength: Short get() =
        if (hasExceptionTable) unsafe.getShort(exceptionTableLengthAddr) else 0

    val exceptionTableLengthAddr: Long get() =
        if (hasCheckedExceptions) checkedExceptionsStart - Short.SIZE_BYTES
        else if (hasMethodParameters) methodParametersStart - Short.SIZE_BYTES
        else if (hasGenericSignature) lastU2Element - Short.SIZE_BYTES
        else lastU2Element

    val checkedExceptionsLengthAddr: Long get() =
        if (hasMethodParameters) methodParametersStart - Short.SIZE_BYTES
        else if (hasGenericSignature) lastU2Element - Short.SIZE_BYTES
        else lastU2Element

}