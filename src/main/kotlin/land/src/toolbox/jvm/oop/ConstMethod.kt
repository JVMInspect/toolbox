package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.dsl.maybeNullArray
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct

class Code(val address: Address, val size: Int) : Scope by address {
    val bytes by lazy {
        unsafe.getMemory(address.base, size)
    }

    operator fun get(index: Int) {
        unsafe.getByte(address.base + index)
    }

    operator fun set(index: Int, value: Byte) {
        unsafe.putByte(address.base + index, value)
    }
}

class ConstMethod(address: Address) : Struct(address) {
    val maxStack: Short by nonNull("_max_stack")
    val maxLocals: Short by nonNull("_max_locals")

    val stackMapData: Array<Byte>? by maybeNullArray("_stackmap_data")
    val hasStackMapTable: Boolean get() = stackMapData != null

    var constants: ConstantPool by nonNull("_constants")

    val nameIndex: Short by nonNull("_name_index")
    val signatureIndex: Short by nonNull("_signature_index")
    val idNum: Short by nonNull("_method_idnum")

    val codeSize: Short by nonNull("_code_size")
    val constMethodSize: Int by nonNull("_constMethod_size")

    val bytecodeOffset: Long get() = type.size.toLong()
    val codeEndOffset: Long get() = bytecodeOffset + codeSize

    val isNative: Boolean get() = method.isNative

    val name: String get() = constants.getString(nameIndex.toInt())
    val signature: String get() = constants.getString(signatureIndex.toInt())

    val code by lazy {
        Code(Address(this, address.base + bytecodeOffset), codeSize.toInt())
    }

    val codeAddress by lazy {
        Address(this, address.base + bytecodeOffset)
    }

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

    val method: Method get() =
        constants.poolHolder.methods[idNum.toInt()]!!

    fun shortAt(offset: Long) = unsafe.getShort(address.base + offset)
    fun addressAt(offset: Long) = unsafe.getAddress(address.base + (offset * pointerSize))

    val lastU2ElementOffset: Long get() {
        var offset = 0
        if (hasMethodAnnotations) offset++
        if (hasParameterAnnotations) offset++
        if (hasTypeAnnotations) offset++
        if (hasDefaultAnnotations) offset++
        return (constMethodSize * pointerSize) - (offset * pointerSize) - Short.SIZE_BYTES
    }

    val genericSignatureOffset: Long get() =
        lastU2ElementOffset

    val genericSignatureIndex: Short get() =
        if (hasGenericSignature) shortAt(genericSignatureOffset) else 0

    val methodAnnotationsOffset: Long get() =
        1

    val methodAnnotations: Array<Byte>? get() =
        if (hasMethodAnnotations) arrays(addressAt(constMethodSize - methodAnnotationsOffset), false)
        else null

    val parameterAnnotationsOffset: Long get() =
        if (hasMethodAnnotations) 1 + methodAnnotationsOffset
        else 1

    val parameterAnnotations: Array<Byte>? get() =
        if (hasParameterAnnotations) arrays(addressAt(constMethodSize - parameterAnnotationsOffset), false)
        else null

    val typeAnnotationsOffset: Long get() =
        if (hasParameterAnnotations) 1 + parameterAnnotationsOffset
        else 1

    val typeAnnotations: Array<Byte>? get() =
        if (hasTypeAnnotations) arrays(addressAt(constMethodSize - typeAnnotationsOffset), false)
        else null

    val defaultAnnotationsOffset: Long get() =
        if (hasTypeAnnotations) typeAnnotationsOffset - 1
        else 1

    val defaultAnnotations: Array<Byte>? get() =
        if (hasDefaultAnnotations) arrays(addressAt(constMethodSize - defaultAnnotationsOffset), false)
        else null

    val methodParametersLengthOffset: Long get() =
        if (hasGenericSignature) lastU2ElementOffset - Short.SIZE_BYTES
        else lastU2ElementOffset

    val methodParametersLength: Short get() =
        if (hasMethodParameters) shortAt(methodParametersLengthOffset) else 0

    val methodParametersOffset: Long get() {
        val offset = methodParametersLengthOffset
        val length = methodParametersLength
        return offset - length * structs.sizeof(MethodParametersElement::class)
    }

    val methodParameters: List<MethodParametersElement> get() {
        val value = mutableListOf<MethodParametersElement>()
        var offset = methodParametersOffset
        val length = methodParametersLength
        for (index in 0 until length) {
            val elementAddress = Address(this, address.base + offset)
            val element = MethodParametersElement(elementAddress)
            value += element
            offset += element.type.size
        }
        return value
    }

    val exceptionTableLengthOffset: Long get() =
        if (hasCheckedExceptions) checkedExceptionsOffset - Short.SIZE_BYTES
        else if (hasMethodParameters) methodParametersOffset - Short.SIZE_BYTES
        else if (hasGenericSignature) lastU2ElementOffset - Short.SIZE_BYTES
        else lastU2ElementOffset

    val exceptionTableLength: Short get() =
        if (hasExceptionTable) shortAt(exceptionTableLengthOffset) else 0

    val exceptionTableOffset: Long get() {
        val offset = exceptionTableLengthOffset
        val length = exceptionTableLength
        return offset - length * structs.sizeof(ExceptionTableElement::class)
    }

    val exceptionTable: List<ExceptionTableElement> get() {
        val value = mutableListOf<ExceptionTableElement>()
        var offset = exceptionTableOffset
        val length = exceptionTableLength
        for (index in 0 until length) {
            val elementAddress = Address(this, address.base + offset)
            val element = ExceptionTableElement(elementAddress)
            value += element
            offset += element.type.size
        }
        return value
    }

    val checkedExceptionsLengthOffset: Long get() =
        if (hasMethodParameters) methodParametersLengthOffset - Short.SIZE_BYTES
        else if (hasGenericSignature) lastU2ElementOffset - Short.SIZE_BYTES
        else lastU2ElementOffset

    val checkedExceptionsLength: Short get() =
        if (hasCheckedExceptions) shortAt(checkedExceptionsLengthOffset) else 0

    val checkedExceptionsOffset: Long get() {
        val offset = checkedExceptionsLengthOffset
        val length = checkedExceptionsLength
        return offset - length * structs.sizeof(CheckedExceptionElement::class)
    }

    val checkedExceptions: List<CheckedExceptionElement> get() {
        val value = mutableListOf<CheckedExceptionElement>()
        var offset = checkedExceptionsOffset
        val length = checkedExceptionsLength
        for (index in 0 until length) {
            val elementAddress = Address(this, address.base + offset)
            val element = CheckedExceptionElement(elementAddress)
            value += element
            offset += element.type.size
        }
        return value
    }

    val compressedLineNumberTableOffset : Long get() =
        codeEndOffset + if (isNative) 2 * pointerSize else 0

    // todo
    //val lineNumberTableLength: Int get() {
    //    var length = 0
    //    if (hasLineNumberTable) {
    //        val address = Address(this, address.base + compressedLineNumberTableOffset)
    //        val stream = CompressedLineNumberReadStream(address)
    //        for (pair in stream) {
    //            length++
    //        }
    //    }
    //    return length
    //}

    val lineNumberTable: List<LineNumberTableElement> get() {
        val value = mutableListOf<LineNumberTableElement>()
        if (!hasLineNumberTable)
            return value

        val address = Address(this, address.base + compressedLineNumberTableOffset)
        val stream = CompressedLineNumberReadStream(address)
        for (element in stream) {
            //println("lnte: ${element.line.toInt()} ${element.bci.toInt()}")
            //println("lnte & 0xff: ${element.line.toInt() and 0xff} bci & 0xff: ${element.bci.toInt() and 0xff}")
            value += element
        }

        return value
    }

    val localVariableTableLengthOffset: Long get() =
        if (hasExceptionTable) exceptionTableOffset - Short.SIZE_BYTES
        else if (hasCheckedExceptions) checkedExceptionsOffset - Short.SIZE_BYTES
        else if (hasMethodParameters) methodParametersOffset - Short.SIZE_BYTES
        else if (hasGenericSignature) lastU2ElementOffset - Short.SIZE_BYTES
        else lastU2ElementOffset

    val localVariableTableLength: Short get() =
        if (hasLocalVariableTable) shortAt(localVariableTableLengthOffset) else 0

    val localVariableTableOffset: Long get() {
        val offset = localVariableTableLengthOffset
        val length = localVariableTableLength
        return offset - length * structs.sizeof(LocalVariableTableElement::class)
    }

    val localVariableTable: List<LocalVariableTableElement> get() {
        val value = mutableListOf<LocalVariableTableElement>()
        var offset = localVariableTableOffset
        val length = localVariableTableLength
        for (index in 0 until length) {
            val elementAddress = Address(this, address.base + offset)
            val element = LocalVariableTableElement(elementAddress)
            value += element
            offset += element.type.size
        }
        return value
    }
}