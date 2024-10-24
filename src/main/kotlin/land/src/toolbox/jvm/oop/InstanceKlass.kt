package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.*
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.ByteArray
import land.src.toolbox.jvm.util.*

class InstanceKlass(address: Address) : Klass(address) {
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

    val sourceFileNameIndex: Short get() = constantPool.sourceFileNameIndex
    val genericSignatureIndex: Short get() = constantPool.genericSignatureIndex

    val annotations: Annotations? by maybeNull("_annotations")

    val innerClasses: Array<Short>? by maybeNullArray("_inner_classes")
    val localInterfaces: Array<InstanceKlass>? by maybeNullArray("_local_interfaces")

    val methods: Array<Method> by nonNullArray("_methods")
    val fieldData: Array<Short> by nonNullArray("_fields")
    val methodOrdering: Array<Int>? by maybeNullArray("_method_ordering")

    val accessFlagsOffset: Int by constant("FieldInfo::access_flags_offset")
    val nameIndexOffset: Int by constant("FieldInfo::name_index_offset")
    val signatureIndexOffset: Int by constant("FieldInfo::signature_index_offset")
    val initialValueIndexOffset: Int by constant("FieldInfo::initval_index_offset")
    val lowPackedOffset: Int by constant("FieldInfo::low_packed_offset")
    val highPackedOffset: Int by constant("FieldInfo::high_packed_offset")
    val miscFlags: Short by nonNull("_misc_flags")

    fun isFlagSet(flag: InstanceKlassFlag): Boolean {
        return (miscFlags.toInt() and flag.bit(this)) != 0
    }

    val fieldInfos: List<FieldInfo> by lazy {
        val info = mutableListOf<FieldInfo>()
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

        for (i in 0 until fieldCount) {
            val accessFlags = fieldData[i * 6 + accessFlagsOffset]!!
            val nameIndex = fieldData[i * 6 + nameIndexOffset]!!
            val signatureIndex = fieldData[i * 6 + signatureIndexOffset]!!
            val initialValIndex = fieldData[i * 6 + initialValueIndexOffset]!!
            val lowOffset = fieldData[i * 6 + lowPackedOffset]!!
            val highOffset = fieldData[i * 6 + highPackedOffset]!!

            info += FieldInfo(
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

    val javaFieldInfos: List<FieldInfo> by lazy {
        fieldInfos.filter { it.accessFlags.toInt() and JVM_ACC_FIELD_INTERNAL == 0 }
    }
}

