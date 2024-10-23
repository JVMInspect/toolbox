package land.src.jvmtb.jvm.oop

import land.src.jvmtb.util.ClassConstants.*
import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.maybeNullArray
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.nonNullArray
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.ByteArray
import land.src.toolbox.jvm.primitive.Oop

class InstanceKlass(address: Address) : Klass(address), Oop {
    val majorVersion: Short get() = constantPool.majorVersion
    val minorVersion: Short get() = constantPool.minorVersion
    val constantPool: ConstantPool by nonNull("_constants")

    val superClass: Klass? by maybeNull("_super")
    val nestHostIndex: Short by lazy {
        val staticOopFieldCountAddress = address.base + type.field("_static_oop_field_count")!!.offsetOrAddress
        unsafe.getShort(staticOopFieldCountAddress - Short.SIZE_BYTES * 2)
    }

    val nestMembers: Array<Short> by lazy {
        val nestMembersAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + pointerSize
        arrays(nestMembersAddress, false)!!
    }

    val permittedSubclasses: Array<Short> by lazy {
        val nestHostAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + (pointerSize * 2)
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        arrays(permittedSubclassesAddress, false)!!
    }

    val recordComponents: Array<RecordComponent>? by lazy {
        val nestHostAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + (pointerSize * 2)
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        val recordComponentsAddress = permittedSubclassesAddress + pointerSize
        if (unsafe.getAddress(recordComponentsAddress) == 0L)
            return@lazy null
        arrays(recordComponentsAddress, true)
    }

    val sourceDebugExtension: String? by lazy {
        val nestHostAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + (pointerSize * 2)
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        val recordComponentsAddress = permittedSubclassesAddress + pointerSize
        val sourceDebugExtensionAddress = recordComponentsAddress + pointerSize
        if (unsafe.getAddress(sourceDebugExtensionAddress) == 0L)
            return@lazy null
        unsafe.getString(unsafe.getAddress(sourceDebugExtensionAddress))
    }

    val genericSignatureIndex: Short get() = constantPool.genericSignatureIndex
    val sourceFileNameIndex: Short get() = constantPool.sourceFileNameIndex//Symbol? by nullableStruct("_source_file_name")

    val _annotations: Annotations? by lazy {
        val annotationsAddress = unsafe.getAddress(address.base + type.field("_annotations")!!.offsetOrAddress)
        Annotations(Address(this, annotationsAddress))
    }

    val annotations: Array<Byte>? get() = _annotations!!.classAnnotations
    val typeAnnotations: Array<Byte>? get() = _annotations!!.classTypeAnnotations
    val fieldsAnnotations: Array<ByteArray>? get() = _annotations!!.fieldsAnnotations
    val fieldsTypeAnnotations: Array<ByteArray>? get() = _annotations!!.fieldsTypeAnnotations

    val innerClasses: Array<Short>? by maybeNullArray("_inner_classes")
    val localInterfaces: Array<InstanceKlass>? by maybeNullArray("_local_interfaces")

    val methods: Array<Method> by nonNullArray("_methods")
    val fields: Array<Short> by nonNullArray("_fields")
    val javaFieldsCount: Short by nonNull("_java_fields_count")
    val methodOrdering: Array<Int>? by maybeNullArray("_method_ordering")

    val fieldInfos: List<FieldInfo> by lazy {
        val info = mutableListOf<FieldInfo>()
        var fieldCount = fields.length

        var index = 0
        while (index < fields.length) {
            val accessFlags: Short = fields[index]!!

            if ((accessFlags.toInt() and JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE) != 0) {
                fieldCount--
            }
            index += 6
        }

        fieldCount /= 6

        for (i in 0 until fieldCount) {
            val accessFlags: Short = fields[i * 6]!!
            val nameIndex: Short = fields[i * 6 + 1]!!
            val signatureIndex: Short = fields[i * 6 + 2]!!
            val initialValIndex: Short = fields[i * 6 + 3]!!
            val lowOffset: Short = fields[i * 6 + 4]!!
            val highOffset: Short = fields[i * 6 + 5]!!

            info += FieldInfo(accessFlags, nameIndex, signatureIndex, initialValIndex, lowOffset, highOffset)
        }

        info
    }

    val javaFieldInfos: List<FieldInfo> by lazy {
        fieldInfos.filter { it.accessFlags.toInt() and JVM_ACC_FIELD_INTERNAL == 0 }
    }
}

