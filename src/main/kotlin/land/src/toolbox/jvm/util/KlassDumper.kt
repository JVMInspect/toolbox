package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.oop.*
import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.oop.InnerClassesIterator
import land.src.toolbox.jvm.primitive.Array
import java.io.DataOutputStream

/**
 * Port of the jvmtiClassFileReconstituter from jvm sources
 */
class KlassDumper(
    val scope: Scope,
    val klass: Klass,
    val buf: DataOutputStream
) {
    val ik = klass.instanceKlass
    val pool = ik.constantPool

    fun writeClassFileFormat() {
        buf.writeInt(0xCAFEBABE.toInt())
        buf.writeShort(ik.majorVersion.toInt())
        buf.writeShort(ik.minorVersion.toInt())

        pool.buildIndices()

        buf.write(pool.bytes)


        buf.writeShort(ik.accessFlags and JVM_RECOGNIZED_CLASS_MODIFIERS)
        buf.writeShort(pool.getClassSymbolIndex(ik.name.string))
        val superIndex = if (ik.superClass == null) 0 else pool.getClassSymbolIndex(ik.superClass!!.name.string)
        buf.writeShort(superIndex )

        val interfaces = ik.localInterfaces
        val numInterfaces = interfaces!!.length
        buf.writeShort(numInterfaces)
        println("interfaces count $numInterfaces")
        for (index in 0 until numInterfaces) {
            val iik = interfaces[index]!!
            buf.writeShort(pool.getUtf8SymbolIndex(iik.name.string))
        }

        writeFieldInfos()
        writeMethodInfos()
        writeClassAttributes()
    }

    fun writeFieldInfos() {
        val fields = ik.javaFieldInfos
        println("fields count: ${fields.size}")
        buf.writeShort(fields.size)
        fields.forEachIndexed { index, info ->
            writeFieldInfo(info, index)
        }
    }

    fun writeFieldInfo(field: FieldInfo, index: Int) {
        val annotations = ik.fieldsAnnotations?.get(index)
        val typeAnnotations = ik.fieldsTypeAnnotations?.get(index)

        buf.writeShort(field.accessFlags.toInt() and JVM_RECOGNIZED_FIELD_MODIFIERS)
        buf.writeShort(field.nameIndex.toInt())
        buf.writeShort(field.signatureIndex.toInt())

        var attributesCount = 0
        if (field.initialValueIndex.toInt() != 0) {
            ++attributesCount
        }
        if (field.signatureIndex.toInt() != 0) {
            ++attributesCount
        }
        if (annotations != null) {
            ++attributesCount
        }
        if (typeAnnotations != null) {
            ++attributesCount
        }

        println("attributes count for field $index: $attributesCount")

        buf.writeShort(attributesCount)

        if (field.initialValueIndex.toInt() != 0) {
            writeAttributeNameIndex("ConstantValue")
            buf.writeInt(2)
            buf.writeShort(field.initialValueIndex.toInt())
        }
        if (field.signatureIndex.toInt() != 0) {
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
        //println("attribute name index for $name is $attributeNameIndex")
        buf.writeShort(attributeNameIndex)
    }

    fun writeClassAttributes() {
        val innerClasses = InnerClassesIterator(scope, ik)
        val genericSignatureIndex = ik.genericSignatureIndex
        val annotations = ik.annotations
        val typeAnnotations = ik.typeAnnotations

        var attributeCount = 0
        if (genericSignatureIndex != 0.toShort()) {
            ++attributeCount
        }
        if (ik.sourceFileNameIndex != 0.toShort()) {
            ++attributeCount
        }
        if (ik.sourceDebugExtension != null) {
            ++attributeCount
        }
        if (innerClasses.entries > 0) {
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
            println("writing generic signature (${pool.getString(genericSignatureIndex.toInt())})")
            writeSignatureAttribute(genericSignatureIndex.toInt())
        }
        if (ik.sourceFileNameIndex != 0.toShort()) {
            println("writing source file")
            writeSourceFileAttribute()
        }
        if (ik.sourceDebugExtension != null) {
            println("writing source file debug extension")
            writeSourceDebugExtensionAttribute()
        }
        if (innerClasses.entries > 0) {
            println("writing ${innerClasses.entries} inner classes")
            writeInnerClassesAttribute(innerClasses)
        }
        if (annotations != null) {
            println("writing ${annotations.length} class annotations")
            writeAnnotationsAttribute("RuntimeVisibleAnnotations", annotations)
        }
        if (typeAnnotations != null) {
            println("writing ${typeAnnotations.length} type annotations")
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
        val operands = ik.constantPool.operands!!
        writeAttributeNameIndex("BootstrapMethods")
        val numBootstrapMethods = pool.operandArrayLength()
        var length = Short.SIZE_BYTES
        for (index in 0 until numBootstrapMethods) {
            val numBootstrapArguments = pool.operandArgumentCount(index)//0 // TODO cpool()->operand_argument_count_at(n);
            length += 2 + 2 + (2 * numBootstrapArguments)
        }
        buf.writeInt(length)
        buf.writeShort(numBootstrapMethods)
        for (index in 0 until numBootstrapMethods) {
            val bootstrapMethodRef = pool.operandBootstrapMethodRefIndex(index) //0 // TODO cpool()->operand_bootstrap_method_ref_index_at(n);
            val numBootstrapArguments = pool.operandArgumentCount(index)//0 // TODO cpool()->operand_argument_count_at(n);
            buf.writeShort(bootstrapMethodRef)
            buf.writeShort(numBootstrapArguments)
            for (argIndex in 0 until numBootstrapArguments) {
                val bootstrapArgument = pool.operandArgumentIndex(index, argIndex) //0 // TODO cpool()->operand_argument_index_at(n, arg);
                buf.writeShort(bootstrapArgument)
            }
        }
    }

    fun writeMethodInfo(method: Method) {
        if (method.constMethod.isOverpass) return

        val accessFlags = method.accessFlags.toInt()
        val constMethod = method.constMethod
        val genericSignatureIndex = constMethod.genericSignatureIndex.toInt()
        val annotations = constMethod.methodAnnotations
        val parameterAnnotations = constMethod.parameterAnnotations
        val defaultAnnotations = constMethod.defaultAnnotations
        val typeAnnotations = constMethod.typeAnnotations

        buf.writeShort(accessFlags and JVM_RECOGNIZED_METHOD_MODIFIERS)
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
            println("writing ${pool.tags[genericSignatureIndex]} signature for ${pool.getString(method.constMethod.nameIndex.toInt())}")
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
        buf.writeInt(checkedExceptionsLength)

        println("writing $checkedExceptionsLength checked exceptions")

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

        val methodOrder = IntArray(numMethods)

        if (ik.methodOrdering != null) {
            for (index in 0 until numMethods) {
                val originalIndex = ik.methodOrdering!![index]!!
                check(originalIndex in 0..<numMethods) {
                    "invalid original method index"
                }
                methodOrder[originalIndex] = index
            }
        }

        for (originalIndex in 0 until numMethods) {
            val index = methodOrder.getOrNull(originalIndex) ?: originalIndex
            writeMethodInfo(methods[index]!!)
        }
    }

    fun writeLineNumberTableAttribute(method: Method, lineNumberCount: Int) {
        //writeAttributeNameIndex("LineNumberTable")
        //buf.writeInt(2 + lineNumberCount * (2 + 2))
        //buf.writeShort(lineNumberCount)
        //val stream = CompressedLineNumberReadStream(method.constMethod.compressedLineNumberTable)
        //for (pair in stream) {
        //    buf.writeShort(pair.bci.toInt())
        //    buf.writeShort(pair.line.toInt())
        //}
    }

    // todo: write rewritten bytecode
    fun copyBytecode(method: Method) {
        val rewriter = CodeRewriter(method.constMethod)
        buf.write(rewriter.rewrittenCode)
        //var s: String?
        //val bytes = scope.unsafe.getMemory(method.address.base + method.constMethod.bytecodeOffset, method.constMethod.codeSize.toInt())
        //buf.write(bytes)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun writeCodeAttribute(method: Method) {
        val constMethod = method.constMethod

        var lineNumberCount = 0
        var stackMapLength = 0
        var localVariableTableLength = 0
        var localVariableTypeTableLength = 0

        var attributeCount = 0
        var attributesSize = 0

        // todo
        //if (constMethod.hasLineNumberTable) {
        //    lineNumberCount = constMethod.lineNumberTableEntries
        //    if (lineNumberCount != 0) {
        //        ++attributeCount
        //        attributesSize += 2 + 4 + 2 + lineNumberCount * (2 + 2)
        //    }
        //}

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

            //println("LVT len: $localVariableTableLength")

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

        val exceptionTableLength = constMethod.exceptionTableLength.toInt()

        val codeSize = constMethod.codeSize
        val size = 2 + 2 + 4 + codeSize + 2 + (2 + 2 + 2 + 2) * exceptionTableLength + 2 + attributesSize

        writeAttributeNameIndex("Code")
        buf.writeInt(size)
        buf.writeShort(method.maxStack.toInt())
        buf.writeShort(method.maxLocals.toInt())

        buf.writeInt(codeSize.toInt())
        copyBytecode(method)

        buf.writeShort(exceptionTableLength)

        for (element in constMethod.exceptionTable) {
            buf.writeShort(element.startPc.toInt())
            buf.writeShort(element.endPc.toInt())
            buf.writeShort(element.handlerPc.toInt())
            buf.writeShort(element.catchTypeIndex.toInt())
        }

        buf.writeShort(attributeCount)
        //if (lineNumberCount != 0) {
        //    writeLineNumberTableAttribute(method, lineNumberCount)
        //}
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
        buf.writeInt(localVariableTypeTableLength)

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
        buf.writeInt(localVariableTableLength)

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