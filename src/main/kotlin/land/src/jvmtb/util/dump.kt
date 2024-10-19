package land.src.jvmtb.util

import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.oop.*
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.util.ClassConstants.JVM_RECOGNIZED_CLASS_MODIFIERS
import land.src.jvmtb.util.ClassConstants.JVM_RECOGNIZED_METHOD_MODIFIERS
import java.io.DataOutputStream

class KlassDumper(val scope: VMScope, val klass: Klass, val buf: DataOutputStream)

fun KlassDumper.writeClassFileFormat() {
    buf.writeInt(0xCAFEBABE.toInt())
    val ik = klass.instanceKlass
    buf.writeShort(ik.majorVersion.toInt())
    buf.writeShort(ik.minorVersion.toInt())

    // write_u2(checked_cast<u2>(cpool()->length()));
    // copy_cpool_bytes(writeable_address(cpool_size()));


    buf.writeShort(ik.accessFlags and JVM_RECOGNIZED_CLASS_MODIFIERS.toInt())
    // write_u2(class_symbol_to_cpool_index(ik()->name()));

    /*
      write_u2(class_symbol_to_cpool_index(ik()->name()));
  Klass* super_class = ik()->super();
  write_u2(super_class == nullptr? 0 :  // zero for java.lang.Object
                class_symbol_to_cpool_index(super_class->name()));
     */

    val interfaces = ik.localInterfaces
    val numInterfaces = interfaces!!.length
    buf.writeShort(numInterfaces)
    for (index in 0 until numInterfaces) {
        val iik = interfaces[index]
        //write_u2(class_symbol_to_cpool_index(iik->name()));
    }

    writeFieldInfos()

    writeMethodInfos()

    writeClassAttributes()
}

fun KlassDumper.writeFieldInfos() {

}

fun KlassDumper.writeAttributeNameIndex(name: String) {
    // todo
}

//u2 JvmtiClassFileReconstituter::inner_classes_attribute_length() {
//    InnerClassesIterator iter(ik());
//    return iter.length();
//}

fun KlassDumper.writeClassAttributes() {
    val ik = klass.instanceKlass
    val innerClasses = InnerClassesIterator(scope, ik)
    val genericSignature = ik.genericSignature
    val annotations = ik.annotations
    val typeAnnotations = ik.typeAnnotations

    var attributeCount = 0
    if (genericSignature != null) {
        ++attributeCount
    }
    if (ik.sourceFileName != null) {
        ++attributeCount
    }
    if (ik.sourceDebugExtension != null) {
        ++attributeCount
    }
    if (innerClasses.length > 0) {
        ++attributeCount
    }
    if (annotations != null) {
        ++attributeCount
    }
    if (typeAnnotations != null) {
        ++attributeCount
    }
    if (ik.constantPool.operands != null) {
        ++attributeCount
    }
    if (ik.nestHostIndex.toInt() != 0) {
        ++attributeCount
    }
    if (ik.nestMembers.length != 0) {
        ++attributeCount
    }
    if (ik.permittedSubclasses.length != 0) {
        ++attributeCount
    }
    if (ik.recordComponents != null) {
        ++attributeCount
    }

    buf.writeShort(attributeCount)

    if (genericSignature != null) {
        // todo: symbol_to_cpool_index(generic_signature)
        writeSignatureAttribute(0)
    }
    if (ik.sourceFileName != null) {
        writeSourceFileAttribute()
    }
    if (ik.sourceDebugExtension != null) {
        writeSourceDebugExtensionAttribute()
    }
    if (innerClasses.length > 0) {
        writeInnerClassesAttribute(innerClasses)
    }
    if (annotations != null) {
        writeAnnotationsAttribute("RuntimeVisibleAnnotations", annotations)
    }
    if (typeAnnotations != null) {
        writeAnnotationsAttribute("RuntimeVisibleTypeAnnotations", typeAnnotations)
    }
    if (ik.constantPool.operands != null) {
        writeBootstrapMethodAttribute()
    }
    if (ik.nestHostIndex.toInt() != 0) {
        writeNestHostAttribute()
    }
    if (ik.nestMembers.length != 0) {
        writeNestMembersAttribute()
    }
    if (ik.permittedSubclasses.length != 0) {
        writePermittedSubclassesAttribute()
    }
    if (ik.recordComponents != null) {
        writeRecordAttribute()
    }
}

fun KlassDumper.writeSourceFileAttribute() {

}

fun KlassDumper.writeSourceDebugExtensionAttribute() {

}

fun KlassDumper.writeInnerClassesAttribute(iterator: InnerClassesIterator) {

}

fun KlassDumper.writeNestHostAttribute() {

}

fun KlassDumper.writeNestMembersAttribute() {

}

fun KlassDumper.writePermittedSubclassesAttribute() {

}

fun KlassDumper.writeRecordAttribute() {

}

fun KlassDumper.writeBootstrapMethodAttribute() {

}

fun KlassDumper.writeMethodInfo(method: Method) {
    if (method.constMethod.isOverpass) return

    val accessFlags = method.accessFlags
    val constMethod = method.constMethod
    val genericSignatureIndex = constMethod.genericSignatureIndex.toInt()
    val annotations = method.constMethod.methodAnnotations
    val parameterAnnotations = method.constMethod.parameterAnnotations
    val defaultAnnotations = method.constMethod.defaultAnnotations
    val typeAnnotations = method.constMethod.typeAnnotations

    buf.writeShort(accessFlags.flags and JVM_RECOGNIZED_METHOD_MODIFIERS.toInt())
    buf.writeShort(constMethod.nameIndex.toInt())
    buf.writeShort(constMethod.signatureIndex.toInt())

    var attributesCount = 0
    if (constMethod.codeSize.toInt() != 0) {
        ++attributesCount
    }
    if (constMethod.hasCheckedExceptions) {
        ++attributesCount // has Exceptions attribute
    }
    if (defaultAnnotations != null) {
        ++attributesCount // has AnnotationDefault attribute
    }
    if (constMethod.hasMethodParameters) {
        ++attributesCount // has MethodParameters attribute
    }
    if (genericSignatureIndex != 0) {
        ++attributesCount
    }
    if (annotations != null) {
        ++attributesCount // has RuntimeVisibleAnnotations attribute
    }
    if (parameterAnnotations != null) {
        ++attributesCount // has RuntimeVisibleParameterAnnotations attribute
    }
    if (typeAnnotations != null) {
        ++attributesCount // has RuntimeVisibleTypeAnnotations attribute
    }

    buf.writeShort(attributesCount)
    if (constMethod.codeSize > 0) {
        writeCodeAttribute(method)
    }
    if (constMethod.hasCheckedExceptions) {
        writeExceptionsAttribute(constMethod)
    }
    if (defaultAnnotations != null) {
        writeAnnotationsAttribute("AnnotationDefault", defaultAnnotations)
    }
    if (constMethod.hasMethodParameters) {
        writeMethodParameterAttribute(constMethod)
    }
    if (genericSignatureIndex != 0) {
        writeSignatureAttribute(genericSignatureIndex)
    }
    if (annotations != null) {
        writeAnnotationsAttribute("RuntimeVisibleAnnotations", annotations)
    }
    if (parameterAnnotations != null) {
        writeAnnotationsAttribute("RuntimeVisibleParameterAnnotations", parameterAnnotations)
    }
    if (typeAnnotations != null) {
        writeAnnotationsAttribute("RuntimeVisibleTypeAnnotations", typeAnnotations)
    }
}

fun KlassDumper.writeSignatureAttribute(genericSignatureIndex: Int) {
    writeAttributeNameIndex("Signature")
    buf.writeInt(2) // always length 2
    buf.writeShort(genericSignatureIndex)
}

fun KlassDumper.writeExceptionsAttribute(method: ConstMethod) {
    val checkedExceptionsLength = method.checkedExceptionsLength.toInt()
    val size = 2 + (2 * checkedExceptionsLength)
    writeAttributeNameIndex("Exceptions")
    buf.writeInt(size)
    buf.writeInt(checkedExceptionsLength)

    val start = method.checkedExceptionsStart
    for (index in 0 until checkedExceptionsLength) {
        val address = start + (index * scope.pointerSize)
        val element = scope.structs<CheckedExceptionElement>(address)!!
        buf.writeShort(element.classCpIndex.toInt())
    }
}

fun KlassDumper.writeMethodParameterAttribute(method: ConstMethod) {
    val length = method.methodParametersLength.toInt()
    val size = 1 + (2 + 2) * length
    writeAttributeNameIndex("MethodParameters")
    buf.writeInt(size)
    buf.writeByte(length)

    val start = method.methodParametersStart
    for (index in 0 until length) {
        val address = start + (index * scope.pointerSize)
        val element = scope.structs<MethodParametersElement>(address)!!
        buf.writeShort(element.nameCpIndex.toInt())
        buf.writeShort(element.flags.toInt())
    }
}

fun KlassDumper.writeAnnotationsAttribute(attributeName: String, annotations: Array<Byte>) {
    writeAttributeNameIndex(attributeName)
    buf.writeShort(annotations.length)
    buf.write(annotations.bytes)
}

fun KlassDumper.writeMethodInfos() {
    val ik = klass.instanceKlass
    val methods = ik.methods

    val numMethods = methods.length
    var numOverpass = 0

    for (index in 0 until numMethods) {
        val method = methods[index]
        if (method.constMethod.isOverpass) {
            numOverpass++
        }
    }

    buf.writeShort(numMethods - numOverpass)

    val methodOrder = IntArray(numMethods)
    for (index in 0 until numMethods) {
        val originalIndex = ik.methodOrdering[index]
        check(originalIndex in 0..<numMethods) {
            "invalid original method index"
        }
        methodOrder[originalIndex] = index
    }

    for (originalIndex in 0 until numMethods) {
        val index = methodOrder[originalIndex]
        writeMethodInfo(methods[index])
    }
}

// todo port CompressedLineNumberReadStream
fun KlassDumper.writeLineNumberTableAttribute(method: Method, lineNumberCount: Int) {
    writeAttributeNameIndex("LineNumberTable")
    buf.writeInt(2 + lineNumberCount * (2 + 2))
    buf.writeShort(lineNumberCount)
    val stream = CompressedLineNumberReadStream(method.constMethod.compressedLineNumberTable)
    for (pair in stream) {
        buf.writeShort(pair.bci.toInt())
        buf.writeShort(pair.line.toInt())
    }
}

// todo: write rewritten bytecode
fun KlassDumper.copyBytecode(method: Method) {

}

fun KlassDumper.writeCodeAttribute(method: Method) {
    val constMethod = method.constMethod

    var lineNumberCount = 0
    var stackMapLength = 0
    var localVariableTableLength = 0
    var localVariableTypeTableLength = 0

    var attributeCount = 0
    var attributesSize = 0

    if (constMethod.hasLineNumberTable) {
        lineNumberCount = constMethod.lineNumberTableEntries
        if (lineNumberCount != 0) {
            ++attributeCount
            attributesSize += 2 + 4 + 2 + lineNumberCount * (2 + 2)
        }
    }

    if (constMethod.hasStackMapTable) {
        stackMapLength = constMethod.stackMapData!!.length
        if (stackMapLength != 0) {
            ++attributeCount
            attributesSize += 2 + 4 + stackMapLength
        }
    }

    if (constMethod.hasLocalVariableTable) {
        localVariableTableLength = constMethod.localVariableTableLength.toInt()
        if (localVariableTableLength != 0) {
            ++attributeCount
            attributesSize += 2 + 4 + 2 + localVariableTableLength * (2 + 2 + 2 + 2 + 2)
        }

        // Local variables with generic signatures must have LVTT entries
        val start = method.constMethod.localVariableTableStart
        for (index in 0 until localVariableTableLength) {
            val address = start + (index * scope.pointerSize)
            val element = scope.structs<LocalVariableTableElement>(address)!!
            if (element.signatureCpIndex != 0.toShort()) {
                localVariableTypeTableLength++
            }
        }

        if (localVariableTypeTableLength != 0) {
            ++attributeCount
            attributesSize += 2 + 4 + 2 + localVariableTypeTableLength * (2 + 2 + 2 + 2 + 2)
        }
    }

    val exceptionTableLength = constMethod.exceptionTableLength.toInt()

    val codeSize = constMethod.codeSize
    val size = 2 + 2 + 4 + codeSize + 2 + (2 + 2 + 2 + 2) * exceptionTableLength + 2 + attributesSize

    writeAttributeNameIndex("Code")
    buf.writeInt(size)
    buf.writeShort(method.maxStack)
    buf.writeShort(method.maxLocals)

    buf.writeInt(codeSize.toInt())
    copyBytecode(method)

    buf.writeShort(exceptionTableLength)
    val start = method.constMethod.exceptionTableStart
    for (index in 0 until exceptionTableLength) {
        val addr = start + (index * scope.pointerSize)
        val element = scope.structs<ExceptionTableElement>(addr)!!
        buf.writeShort(element.startPc.toInt())
        buf.writeShort(element.endPc.toInt())
        buf.writeShort(element.handlerPc.toInt())
        buf.writeShort(element.catchTypeIndex.toInt())
    }

    buf.writeShort(attributeCount)
    if (lineNumberCount != 0) {
        writeLineNumberTableAttribute(method, lineNumberCount)
    }
    if (stackMapLength != 0) {
        writeStackMapTableAttribute(method, stackMapLength)
    }
    if (localVariableTableLength != 0) {
        writeLocalVariableTableAttribute(method, localVariableTableLength)
    }
    if (localVariableTypeTableLength != 0) {
        writeLocalVariableTypeTableAttribute(method, localVariableTypeTableLength)
    }
}

fun KlassDumper.writeLocalVariableTypeTableAttribute(method: Method, localVariableTypeTableLength: Int) {
    writeAttributeNameIndex("LocalVariableTypeTable")
    buf.writeInt(2 + localVariableTypeTableLength * (2 + 2 + 2 + 2 + 2))
    buf.writeInt(localVariableTypeTableLength)

    val start = method.constMethod.localVariableTableStart
    for (index in 0 until method.constMethod.localVariableTableLength) {
        val address = start + (index * scope.pointerSize)
        val element = scope.structs<LocalVariableTableElement>(address)!!
        if (element.signatureCpIndex > 0.toShort()) {
            buf.writeShort(element.startBci.toInt())
            buf.writeShort(element.length.toInt())
            buf.writeShort(element.nameCpIndex.toInt())
            buf.writeShort(element.descriptorCpIndex.toInt())
            buf.writeShort(element.slot.toInt())
        }
    }
}

fun KlassDumper.writeLocalVariableTableAttribute(method: Method, localVariableTableLength: Int) {
    writeAttributeNameIndex("LocalVariableTable")
    buf.writeInt(2 + localVariableTableLength * (2 + 2 + 2 + 2 + 2))
    buf.writeInt(localVariableTableLength)

    val start = method.constMethod.localVariableTableStart
    for (index in 0 until localVariableTableLength) {
        val address = start + (index * scope.pointerSize)
        val element = scope.structs<LocalVariableTableElement>(address)!!
        buf.writeShort(element.startBci.toInt())
        buf.writeShort(element.length.toInt())
        buf.writeShort(element.nameCpIndex.toInt())
        buf.writeShort(element.descriptorCpIndex.toInt())
        buf.writeShort(element.slot.toInt())
    }
}

fun KlassDumper.writeStackMapTableAttribute(method: Method, stackMapLength: Int) {
    writeAttributeNameIndex("StackMapTable")
    buf.writeInt(stackMapLength)
    buf.write(method.constMethod.stackMapData!!.bytes)
}
