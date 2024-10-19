package land.src.jvmtb.util

import land.src.jvmtb.jvm.oop.Annotation
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.jvm.oop.ConstMethod
import land.src.jvmtb.jvm.oop.Klass
import land.src.jvmtb.jvm.oop.Method
import land.src.jvmtb.util.ClassConstants.JVM_RECOGNIZED_CLASS_MODIFIERS
import land.src.jvmtb.util.ClassConstants.JVM_RECOGNIZED_METHOD_MODIFIERS
import java.io.DataOutputStream

class KlassDumper(val klass: Klass, val buf: DataOutputStream)

fun KlassDumper.writeClassFileFormat() {
    buf.writeInt(0xCAFEBABE.toInt())
    val ik = klass.instanceKlass
    buf.writeShort(ik.majorVersion)
    buf.writeShort(ik.minorVersion)

   // write_u2(checked_cast<u2>(cpool()->length()));
   // copy_cpool_bytes(writeable_address(cpool_size()));


    buf.writeShort(ik.accessFlags.flags and JVM_RECOGNIZED_CLASS_MODIFIERS.toInt())
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

fun KlassDumper.writeClassAttributes() {
    //u2 inner_classes_length = inner_classes_attribute_length();
    //Symbol* generic_signature = ik()->generic_signature();
    //AnnotationArray* anno = ik()->class_annotations();
    //AnnotationArray* type_anno = ik()->class_type_annotations();
}

fun KlassDumper.writeMethodInfo(method: Method) {
    if (method.isOverpass) return

    val accessFlags = method.accessFlags
    val constMethod = method.constMethod
    val genericSignatureIndex = constMethod.genericSignatureIndex
    val annotations = method.annotations
    val parameterAnnotations = method.parameterAnnotations
    val defaultAnnotations = method.defaultAnnotations
    val typeAnnotations = method.typeAnnotations

    buf.writeShort(accessFlags.flags and JVM_RECOGNIZED_METHOD_MODIFIERS.toInt())
    buf.writeShort(constMethod.nameIndex)
    buf.writeShort(constMethod.signatureIndex)

    var attributesCount = 0
    if (constMethod.codeSize != 0) {
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

}

fun KlassDumper.writeMethodParameterAttribute(method: ConstMethod) {

}

fun KlassDumper.writeAnnotationsAttribute(attributeName: String, annotations: Array<Annotation>) {
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
        if (method.isOverpass) {
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

// todo write line number table attribute
fun KlassDumper.writeLineNumberTableAttribute(method: Method, lineNumberCount: Int) {

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
        stackMapLength = constMethod.stackMapData.size
        if (stackMapLength != 0) {
            ++attributeCount
            attributesSize += 2 + 4 + stackMapLength
        }
    }

    if (constMethod.hasLocalVariableTable) {
        localVariableTableLength = constMethod.localVariableTableLength
        if (localVariableTableLength != 0) {
            ++attributeCount
            attributesSize += 2 + 4 + 2 + localVariableTableLength * (2 + 2 + 2 + 2 + 2)
        }

        // todo iterate local variable type table

        if (localVariableTypeTableLength != 0) {
            ++attributeCount
            attributesSize += 2 + 4 + 2 + localVariableTypeTableLength * (2 + 2 + 2 + 2 + 2);
        }
    }

    // todo: read exception table
    var exceptionTableLength = 1

    val codeSize = constMethod.codeSize
    val size = 2 + 2 + 4 + codeSize + 2 + (2 + 2 + 2 + 2) * exceptionTableLength + 2 + attributesSize;

    // todo: write Code attribute name index
    buf.writeInt(size)
    buf.writeShort(method.maxStack)
    buf.writeShort(method.maxLocals)
    buf.writeInt(codeSize)
    copyBytecode(method)
    buf.writeShort(exceptionTableLength)
    for (index in 0 until exceptionTableLength) {
        // buf.writeShort(exception_table.start_pc(index))
        // buf.writeShort(exception_table.end_pc(index))
        // buf.writeShort(exception_table.handler_pc(index))
        // buf.writeShort(exception_table.catch_type_index(index))
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

// todo write local variable type table attribute
fun KlassDumper.writeLocalVariableTypeTableAttribute(method: Method, localVariableTypeTableLength: Int) {

}

// todo write local type table attribute
fun KlassDumper.writeLocalVariableTableAttribute(method: Method, localVariableTableLength: Int) {

}

// todo write stack map table attribute
fun KlassDumper.writeStackMapTableAttribute(method: Method, stackMapLength: Int) {
    writeAttributeNameIndex("StackMap")
}
