package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.util.ClassConstants.*
import land.src.toolbox.jvm.dsl.*
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.ByteArray

class InstanceKlass(address: Address) : land.src.toolbox.jvm.oop.Klass(address) {
    val majorVersion: Short get() = constantPool.majorVersion
    val minorVersion: Short get() = constantPool.minorVersion
    val constantPool: land.src.toolbox.jvm.oop.ConstantPool by nonNull("_constants")

    val superClass: land.src.toolbox.jvm.oop.Klass? by maybeNull("_super")

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

    val recordComponents: Array<land.src.toolbox.jvm.oop.RecordComponent>? by lazy {
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

    val _annotations: land.src.toolbox.jvm.oop.Annotations? by lazy {
        val annotationsAddress = unsafe.getAddress(address.base + type.field("_annotations")!!.offsetOrAddress)
        land.src.toolbox.jvm.oop.Annotations(Address(this, annotationsAddress))
    }

    val annotations: Array<Byte>? get() = _annotations!!.classAnnotations
    val typeAnnotations: Array<Byte>? get() = _annotations!!.classTypeAnnotations
    val fieldsAnnotations: Array<ByteArray>? get() = _annotations!!.fieldsAnnotations
    val fieldsTypeAnnotations: Array<ByteArray>? get() = _annotations!!.fieldsTypeAnnotations

    val innerClasses: Array<Short>? by maybeNullArray("_inner_classes")
    val localInterfaces: Array<land.src.toolbox.jvm.oop.InstanceKlass>? by maybeNullArray("_local_interfaces")

    val methods: Array<land.src.toolbox.jvm.oop.Method> by nonNullArray("_methods")
    val fieldData: Array<Short> by nonNullArray("_fields")
    val javaFieldsCount: Short by nonNull("_java_fields_count")
    val methodOrdering: Array<Int>? by maybeNullArray("_method_ordering")

    val accessFlagsOffset: Int by constant("FieldInfo::access_flags_offset")
    val nameIndexOffset: Int by constant("FieldInfo::name_index_offset")
    val signatureIndexOffset: Int by constant("FieldInfo::signature_index_offset")
    val initialValueIndexOffset: Int by constant("FieldInfo::initval_index_offset")
    val lowPackedOffset: Int by constant("FieldInfo::low_packed_offset")
    val highPackedOffset: Int by constant("FieldInfo::high_packed_offset")

    val fieldInfos: List<land.src.toolbox.jvm.oop.FieldInfo> by lazy {
        val info = mutableListOf<land.src.toolbox.jvm.oop.FieldInfo>()
        var fieldCount = fieldData.length

        var index = 0
        while (index < fieldData.length) {
            val accessFlags: Short = fieldData[index]!!

            if ((accessFlags.toInt() and JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE) != 0) {
                fieldCount--
            }
            index += 6
        }

        fieldCount /= 6

        //check(fieldCount == javaFieldsCount.toInt()) {
        //    "fieldCount ($fieldCount) != javaFieldsCount ($javaFieldsCount)"
        //}

        for (i in 0 until fieldCount) {
            val accessFlags = fieldData[i * 6 + accessFlagsOffset]!!
            val nameIndex = fieldData[i * 6 + nameIndexOffset]!!
            val signatureIndex = fieldData[i * 6 + signatureIndexOffset]!!
            val initialValIndex = fieldData[i * 6 + initialValueIndexOffset]!!
            val lowOffset = fieldData[i * 6 + lowPackedOffset]!!
            val highOffset = fieldData[i * 6 + highPackedOffset]!!

            info += land.src.toolbox.jvm.oop.FieldInfo(
                accessFlags,
                nameIndex,
                signatureIndex,
                initialValIndex,
                lowOffset,
                highOffset
            )
        }

        info
    }

    val javaFieldInfos: List<land.src.toolbox.jvm.oop.FieldInfo> by lazy {
        fieldInfos.filter { it.accessFlags.toInt() and JVM_ACC_FIELD_INTERNAL == 0 }
    }
}

