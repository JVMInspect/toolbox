package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.oop.*
import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.oop.InnerClassesIterator
import land.src.toolbox.jvm.primitive.Array
import java.io.DataOutputStream

enum class DumpMode {
    Full,
    Minimal
}

/**
 * Port of the jvmtiClassFileReconstituter from jvm sources
 */
class KlassDumper(
    val scope: Scope,
    val klass: Klass,
    val buf: DataOutputStream,
    val mode: DumpMode = DumpMode.Full
) {
    val ik = klass.instanceKlass
    val pool = ik.constantPool

    fun writeClassFileFormat() {
        buf.writeInt(0xCAFEBABE.toInt())
        buf.writeShort(ik.minorVersion.toInt())
        buf.writeShort(ik.majorVersion.toInt())

        pool.buildIndices()

        buf.write(pool.bytes)

        buf.writeShort(ik.accessFlags and JVM_RECOGNIZED_CLASS_MODIFIERS)
        buf.writeShort(pool.getClassSymbolIndex(ik.name.string))
        val superIndex = if (ik.superClass == null) 0 else pool.getClassSymbolIndex(ik.superClass!!.name.string)
        buf.writeShort(superIndex )

        val interfaces = ik.localInterfaces
        val numInterfaces = interfaces!!.length
        buf.writeShort(numInterfaces)
        for (index in 0 until numInterfaces) {
            val iik = interfaces[index]!!
            buf.writeShort(pool.getClassSymbolIndex(iik.name.string))
        }

        writeFieldInfos()
        writeMethodInfos()
        writeClassAttributes()
    }

    fun writeFieldInfos() {
        val fields = ik.javaFieldInfos
        buf.writeShort(fields.size)
        fields.forEachIndexed { index, info ->
            writeFieldInfo(info, index)
        }
    }

    fun writeFieldInfo(field: FieldInfo, index: Int) {
        val annotations = if (mode == DumpMode.Minimal) null else ik.annotations?.fieldsAnnotations?.get(index)
        val typeAnnotations = if (mode == DumpMode.Minimal) null else ik.annotations?.fieldsTypeAnnotations?.get(index)

        buf.writeShort(field.accessFlags.toInt() and JVM_RECOGNIZED_FIELD_MODIFIERS)
        buf.writeShort(field.nameIndex.toInt())
        buf.writeShort(field.signatureIndex.toInt())

        val signatureIndex = if (mode == DumpMode.Minimal) 0 else field.signatureIndex.toInt()
        val initialValueIndex = field.initialValueIndex.toInt()

        var attributesCount = 0
        if (initialValueIndex != 0) {
            ++attributesCount
        }
        if (signatureIndex != 0) {
            ++attributesCount
        }
        if (annotations != null) {
            ++attributesCount
        }
        if (typeAnnotations != null) {
            ++attributesCount
        }

        buf.writeShort(attributesCount)

        if (initialValueIndex != 0) {
            writeAttributeNameIndex("ConstantValue")
            buf.writeInt(2)
            buf.writeShort(field.initialValueIndex.toInt())
        }
        if (signatureIndex != 0) {
            writeSignatureAttribute(field.signatureIndex.toInt())
        }
        if (annotations != null) {
            writeAnnotationsAttribute("RuntimeVisibleAnnotations", annotations)
        }
        if (typeAnnotations != null) {
            writeAnnotationsAttribute("RuntimeVisibleTypeAnnotations", typeAnnotations)
        }
    }

    fun writeAttributeNameIndex(name: String) {
        val attributeNameIndex = pool.getUtf8SymbolIndex(name)
        buf.writeShort(attributeNameIndex)
    }

    fun writeClassAttributes() {
        val innerClasses = InnerClassesIterator(scope, ik)
        val innerClassEntries = if (mode == DumpMode.Minimal) null else innerClasses.entries
        val genericSignatureIndex = if (mode == DumpMode.Minimal) 0 else ik.genericSignatureIndex
        val annotations = if (mode == DumpMode.Minimal) null else ik.annotations?.classAnnotations
        val typeAnnotations = if (mode == DumpMode.Minimal) null else ik.annotations?.classTypeAnnotations
        val sourceFileNameIndex = if (mode == DumpMode.Minimal) 0 else ik.sourceFileNameIndex.toInt()
        val sourceDebugExtension = if (mode == DumpMode.Minimal) null else ik.sourceDebugExtension

        var attributeCount = 0
        if (genericSignatureIndex != 0.toShort()) {
            ++attributeCount
        }
        if (sourceFileNameIndex != 0) {
            ++attributeCount
        }
        if (sourceDebugExtension != null) {
            ++attributeCount
        }
        if ((innerClassEntries ?: 0) > 0) {
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
        //if (ik.nestHostIndex.toInt() != 0) {
        //    ++attributeCount
        //}
        //if (ik.nestMembers.length != 0) {
        //    ++attributeCount
        //}
        //if (ik.permittedSubclasses.length != 0) {
        //    ++attributeCount
        //}
        //if (ik.recordComponents != null) {
        //    ++attributeCount
        //}

        buf.writeShort(attributeCount)

        if (genericSignatureIndex != 0.toShort()) {
            writeSignatureAttribute(genericSignatureIndex.toInt())
        }
        if (sourceFileNameIndex != 0) {
            writeSourceFileAttribute()
        }
        if (sourceDebugExtension != null) {
            writeSourceDebugExtensionAttribute()
        }
        if ((innerClassEntries ?: 0) > 0) {
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
        //if (ik.nestHostIndex.toInt() != 0) {
        //    writeNestHostAttribute()
        //}
        //if (ik.nestMembers.length != 0) {
        //    writeNestMembersAttribute()
        //}
        //if (ik.permittedSubclasses.length != 0) {
        //    writePermittedSubclassesAttribute()
        //}
        //if (ik.recordComponents != null) {
        //    writeRecordAttribute()
        //}
    }

    fun writeSourceFileAttribute() {
        writeAttributeNameIndex("SourceFile")
        buf.writeInt(2)
        buf.writeShort(pool.sourceFileNameIndex.toInt())
    }

    fun writeSourceDebugExtensionAttribute() {
        writeAttributeNameIndex("SourceDebugExtension")
        val length = ik.sourceDebugExtension!!.length
        buf.writeInt(length)
        buf.write(ik.sourceDebugExtension!!.toByteArray())
    }

    fun writeInnerClassesAttribute(iterator: InnerClassesIterator) {
        val entryCount = iterator.entries
        val size = 2 + entryCount * (2 + 2 + 2 + 2)
        writeAttributeNameIndex("InnerClasses")
        buf.writeInt(size)
        buf.writeShort(entryCount)
        for (info in iterator) {
            buf.writeShort(info.classInfo.toInt())
            buf.writeShort(info.outerClassInfo.toInt())
            buf.writeShort(info.innerName.toInt())
            buf.writeShort(info.accessFlags.toInt())
        }
    }

    fun writeNestHostAttribute() {
        writeAttributeNameIndex("NestHost")
        buf.writeInt(2)
        buf.writeShort(ik.nestHostIndex.toInt())
    }

    fun writeNestMembersAttribute() {
        val nestMembers = ik.nestMembers
        val numberOfClasses = nestMembers.length
        val length = 2 * (1 + numberOfClasses)

        writeAttributeNameIndex("NestMembers")
        buf.writeInt(length)
        buf.writeShort(numberOfClasses)
        for (index in 0 until numberOfClasses) {
            val classIndex = nestMembers[index]!!
            buf.writeShort(classIndex.toInt())
        }
    }

    fun writePermittedSubclassesAttribute() {
        val permittedSubclasses = ik.permittedSubclasses
        val numberOfClasses = permittedSubclasses.length
        val length = 2 * (1 + numberOfClasses)

        println("number of permitted subclasses $numberOfClasses")

        writeAttributeNameIndex("PermittedSubclasses")
        buf.writeInt(length)
        buf.writeShort(numberOfClasses)
        for (index in 0 until numberOfClasses) {
            val classIndex = permittedSubclasses[index]!!
            buf.writeShort(classIndex.toInt())
        }
    }

    fun writeRecordAttribute() {
        val components = ik.recordComponents
        val numberOfComponents = components!!.length

        var length = 2 + (2 * 3 * numberOfComponents)
        for (index in 0 until numberOfComponents) {
            val component = components[index]!!
            if (component.genericSignatureIndex != 0.toShort()) {
                length += 8
            }
            if (component.annotations != null) {
                length += 6 + component.annotations!!.length
            }
            if (component.typeAnnotations != null) {
                length += 6 + component.typeAnnotations!!.length
            }
        }

        writeAttributeNameIndex("Record")
        buf.writeInt(length)
        buf.writeShort(numberOfComponents)
        for (index in 0 until numberOfComponents) {
            val component = components[index]!!
            buf.writeShort(component.nameIndex.toInt())
            buf.writeShort(component.descriptorIndex.toInt())
            buf.writeShort(component.attributesCount.toInt())
            if (component.genericSignatureIndex != 0.toShort()) {
                writeSignatureAttribute(component.genericSignatureIndex.toInt())
            }
            if (component.annotations != null) {
                writeAnnotationsAttribute("RuntimeVisibleAnnotations", component.annotations!!)
            }
            if (component.typeAnnotations != null) {
                writeAnnotationsAttribute("RuntimeVisibleTypeAnnotations", component.typeAnnotations!!)
            }
        }
    }

    fun writeBootstrapMethodAttribute() {
        writeAttributeNameIndex("BootstrapMethods")
        val numBootstrapMethods = pool.operandArrayLength()
        var length = Short.SIZE_BYTES
        for (index in 0 until numBootstrapMethods) {
            val numBootstrapArguments = pool.operandArgumentCount(index)
            length += 2 + 2 + (2 * numBootstrapArguments)
        }
        buf.writeInt(length)
        buf.writeShort(numBootstrapMethods)
        for (index in 0 until numBootstrapMethods) {
            val bootstrapMethodRef = pool.operandBootstrapMethodRefIndex(index)
            val numBootstrapArguments = pool.operandArgumentCount(index)
            buf.writeShort(bootstrapMethodRef)
            buf.writeShort(numBootstrapArguments)
            for (argIndex in 0 until numBootstrapArguments) {
                val bootstrapArgument = pool.operandArgumentIndex(index, argIndex)
                buf.writeShort(bootstrapArgument)
            }
        }
    }

    fun writeMethodInfo(method: Method) {
        if (method.constMethod.isOverpass) return

        val accessFlags = method.accessFlags.toInt()
        val constMethod = method.constMethod
        val genericSignatureIndex = if (mode == DumpMode.Minimal) 0 else constMethod.genericSignatureIndex.toInt()
        val annotations = if (mode == DumpMode.Minimal) null else constMethod.methodAnnotations
        val parameterAnnotations = if (mode == DumpMode.Minimal) null else constMethod.parameterAnnotations
        val defaultAnnotations = if (mode == DumpMode.Minimal) null else constMethod.defaultAnnotations
        val typeAnnotations = if (mode == DumpMode.Minimal) null else constMethod.typeAnnotations
        val hasMethodParameters = if (mode == DumpMode.Minimal) false else constMethod.hasCheckedExceptions
        val hasCheckedExceptions = if (mode == DumpMode.Minimal) false else constMethod.hasCheckedExceptions

        buf.writeShort(accessFlags and JVM_RECOGNIZED_METHOD_MODIFIERS)
        buf.writeShort(constMethod.nameIndex.toInt())
        buf.writeShort(constMethod.signatureIndex.toInt())

        var attributesCount = 0
        if (constMethod.codeSize > 0) {
            ++attributesCount
        }
        if (hasCheckedExceptions) {
            ++attributesCount // has Exceptions attribute
        }
        if (defaultAnnotations != null) {
            ++attributesCount // has AnnotationDefault attribute
        }
        if (hasMethodParameters) {
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
        if (hasCheckedExceptions) {
            writeExceptionsAttribute(constMethod)
        }
        if (defaultAnnotations != null) {
            writeAnnotationsAttribute("AnnotationDefault", defaultAnnotations)
        }
        if (hasMethodParameters) {
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

    fun writeSignatureAttribute(genericSignatureIndex: Int) {
        writeAttributeNameIndex("Signature")
        buf.writeInt(2) // always length 2
        buf.writeShort(genericSignatureIndex)
    }

    fun writeExceptionsAttribute(method: ConstMethod) {
        val checkedExceptionsLength = method.checkedExceptionsLength.toInt()
        val size = 2 + (2 * checkedExceptionsLength)
        writeAttributeNameIndex("Exceptions")
        buf.writeInt(size)
        buf.writeShort(checkedExceptionsLength)

        for (element in method.checkedExceptions) {
            buf.writeShort(element.classCpIndex.toInt())
        }
    }

    fun writeMethodParameterAttribute(method: ConstMethod) {
        val length = method.methodParametersLength.toInt()
        val size = 1 + (2 + 2) * length
        writeAttributeNameIndex("MethodParameters")
        buf.writeInt(size)
        buf.writeByte(length)

        println("writing $length method parameters")

        for (element in method.methodParameters) {
            buf.writeShort(element.nameCpIndex.toInt())
            buf.writeShort(element.flags.toInt())
        }
    }

    fun writeAnnotationsAttribute(attributeName: String, annotations: Array<Byte>) {
        writeAttributeNameIndex(attributeName)
        buf.writeInt(annotations.length)
        buf.write(annotations.bytes)
    }

    fun writeMethodInfos() {
        val ik = klass.instanceKlass
        val methods = ik.methods

        val numMethods = methods.length
        var numOverpass = 0

        for (index in 0 until numMethods) {
            val method = methods[index]!!
            if (method.constMethod.isOverpass) {
                numOverpass++
            }
        }

        buf.writeShort(numMethods - numOverpass)

        var methodOrder = arrayOfNulls<Int>(numMethods)

        if (ik.methodOrdering != null) {
            for (index in 0 until numMethods) {
                val originalIndex = ik.methodOrdering!![index]!!
                if (originalIndex !in 0..<numMethods) {
                    println("method order link was broken $originalIndex !in 0..<$numMethods reverting to default.")
                    methodOrder = arrayOfNulls(numMethods)
                    break
                }
                methodOrder[originalIndex] = index
            }
        }

        for (originalIndex in 0 until numMethods) {
            val index = methodOrder.getOrNull(originalIndex) ?: originalIndex
            writeMethodInfo(methods[index]!!)
        }
    }

    fun writeLineNumberTableAttribute(constMethod: ConstMethod, lineNumberCount: Int) {
        writeAttributeNameIndex("LineNumberTable")
        buf.writeInt(2 + lineNumberCount * (2 + 2))
        buf.writeShort(lineNumberCount)
        val elements = constMethod.lineNumberTable
        for (element in elements) {
            buf.writeShort(element.bci.toInt())
            buf.writeShort(element.line.toInt())
        }
    }

    fun copyBytecode(method: Method) {
        val rewriter = CodeRewriter(method.constMethod)
        buf.write(rewriter.rewrittenCode)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun writeCodeAttribute(method: Method) {
        val constMethod = method.constMethod

        var lineNumberCount = 0
        var stackMapLength = 0
        var localVariableTableLength = 0
        var localVariableTypeTableLength = 0

        val hasLineNumberTable = if (mode == DumpMode.Minimal) false else constMethod.hasLineNumberTable
        val hasStackMapTable = if (mode == DumpMode.Minimal) false else constMethod.hasStackMapTable
        val hasLocalVariableTable = if (mode == DumpMode.Minimal) false else constMethod.hasLocalVariableTable
        val exceptionTable = if (mode == DumpMode.Minimal) emptyList() else constMethod.exceptionTable

        var attributeCount = 0
        var attributesSize = 0

        // todo
        if (hasLineNumberTable) {
            lineNumberCount = constMethod.lineNumberTable.size
            if (lineNumberCount != 0) {
                ++attributeCount
                attributesSize += 2 + 4 + 2 + lineNumberCount * (2 + 2)
            }
        }

        if (hasStackMapTable) {
            stackMapLength = constMethod.stackMapData!!.length
            if (stackMapLength != 0) {
                ++attributeCount
                attributesSize += 2 + 4 + stackMapLength
            }
        }

        if (hasLocalVariableTable) {
            localVariableTableLength = constMethod.localVariableTableLength.toInt()
            if (localVariableTableLength != 0) {
                ++attributeCount
                attributesSize += 2 + 4 + 2 + localVariableTableLength * (2 + 2 + 2 + 2 + 2)
            }

            // Local variables with generic signatures must have LVTT entries
            for (element in constMethod.localVariableTable) {
                if (element.signatureCpIndex != 0.toShort()) {
                    localVariableTypeTableLength++
                }
            }

            if (localVariableTypeTableLength != 0) {
                ++attributeCount
                attributesSize += 2 + 4 + 2 + localVariableTypeTableLength * (2 + 2 + 2 + 2 + 2)
            }
        }

        val codeSize = constMethod.codeSize
        val size = 2 + 2 + 4 + codeSize + 2 + (2 + 2 + 2 + 2) * exceptionTable.size + 2 + attributesSize

        writeAttributeNameIndex("Code")
        buf.writeInt(size)
        buf.writeShort(method.maxStack.toInt())
        buf.writeShort(method.maxLocals.toInt())

        buf.writeInt(codeSize.toInt())
        copyBytecode(method)

        buf.writeShort(exceptionTable.size)

        for (element in exceptionTable) {
            buf.writeShort(element.startPc.toInt())
            buf.writeShort(element.endPc.toInt())
            buf.writeShort(element.handlerPc.toInt())
            buf.writeShort(element.catchTypeIndex.toInt())
        }

        buf.writeShort(attributeCount)
        if (lineNumberCount != 0) {
            writeLineNumberTableAttribute(constMethod, lineNumberCount)
        }
        if (stackMapLength != 0) {
            writeStackMapTableAttribute(constMethod, stackMapLength)
        }
        if (localVariableTableLength != 0) {
            writeLocalVariableTableAttribute(constMethod, localVariableTableLength)
        }
        if (localVariableTypeTableLength != 0) {
            writeLocalVariableTypeTableAttribute(constMethod, localVariableTypeTableLength)
        }
    }

    fun writeLocalVariableTypeTableAttribute(constMethod: ConstMethod, localVariableTypeTableLength: Int) {
        writeAttributeNameIndex("LocalVariableTypeTable")
        buf.writeInt(2 + localVariableTypeTableLength * (2 + 2 + 2 + 2 + 2))
        buf.writeShort(localVariableTypeTableLength)

        for (element in constMethod.localVariableTable) {
            if (element.signatureCpIndex > 0.toShort()) {
                buf.writeShort(element.startBci.toInt())
                buf.writeShort(element.length.toInt())
                buf.writeShort(element.nameCpIndex.toInt())
                buf.writeShort(element.descriptorCpIndex.toInt())
                buf.writeShort(element.slot.toInt())
            }
        }
    }

    fun writeLocalVariableTableAttribute(constMethod: ConstMethod, localVariableTableLength: Int) {
        writeAttributeNameIndex("LocalVariableTable")
        buf.writeInt(2 + localVariableTableLength * (2 + 2 + 2 + 2 + 2))
        buf.writeShort(localVariableTableLength)

        for (element in constMethod.localVariableTable) {
            buf.writeShort(element.startBci.toInt())
            buf.writeShort(element.length.toInt())
            buf.writeShort(element.nameCpIndex.toInt())
            buf.writeShort(element.descriptorCpIndex.toInt())
            buf.writeShort(element.slot.toInt())
        }
    }

    fun writeStackMapTableAttribute(constMethod: ConstMethod, stackMapLength: Int) {
        writeAttributeNameIndex("StackMapTable")
        buf.writeInt(stackMapLength)
        buf.write(constMethod.stackMapData!!.bytes)
    }

}