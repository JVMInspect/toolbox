package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.*
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.util.*

class InstanceKlass(address: Address) : Klass(address) {
    val majorVersion: Short get() = constantPool.majorVersion
    val minorVersion: Short get() = constantPool.minorVersion
    var constantPool: ConstantPool by nonNull("_constants")

    val superClass: Klass? by maybeNull("_super")

    val nestHostIndex: Short by lazy {
        val staticOopFieldCountAddress = address.base + type.field("_static_oop_field_count")!!.offsetOrAddress
        unsafe.getShort(staticOopFieldCountAddress - Short.SIZE_BYTES * 2)
    }

    val nestMembers: Array<Short> by lazy {
        val nestMembersAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + pointerSize
        arrays(nestMembersAddress, false)!!
    }

    val nestHost: InstanceKlass? by lazy {
        val nestHostAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + (pointerSize * 2)
        if (unsafe.getAddress(nestHostAddress) == 0L)
            return@lazy null
        InstanceKlass(Address(this, nestHostAddress))
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

    var osrNMethodsHead: NMethod? by maybeNull("_osr_nmethods_head")

    fun isFlagSet(flag: InstanceKlassFlag): Boolean {
        return (miscFlags.toInt() and flag.bit(this)) != 0
    }

    fun findMethod(name: String, descriptor: String): Method? {
        return methods.firstOrNull {
            val methodName = constantPool.getString(it.constMethod.nameIndex.toInt())
            val methodDescriptor = constantPool.getString(it.constMethod.signatureIndex.toInt())
            methodName == name && methodDescriptor == descriptor
        }
    }

    fun getFieldName(info: FieldInfo): String {
        return if (info.accessFlags.toInt() and JVM_ACC_FIELD_INTERNAL != 0) {
            globals.vmSymbols.lookupSymbol(info.nameIndex.toInt()).string
        } else {
            constantPool.getString(info.nameIndex.toInt())
        }
    }

    fun getFieldDescriptor(info: FieldInfo): String {
        return if (info.accessFlags.toInt() and JVM_ACC_FIELD_INTERNAL != 0) {
            globals.vmSymbols.lookupSymbol(info.signatureIndex.toInt()).string
        } else {
            constantPool.getString(info.signatureIndex.toInt())
        }
    }

    fun getField(name: String): FieldInfo {
        return fieldInfos.first { getFieldName(it) == name }
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

    val resolvedFields: List<Field> by lazy {
        fieldInfos.map {
            val name = getFieldName(it)
            val descriptor = getFieldDescriptor(it)
            val offset = it.highOffset.toInt() shl 16 or it.lowOffset.toInt()
            Field(name, descriptor, it.accessFlags.toInt(), offset shr 2)
        }
    }

    fun removeOsrNMethod(n: NMethod): Boolean {
        var found = false

        var cur = osrNMethodsHead
        var last: NMethod? = null

        val m = n.method!!

        while(cur != null && cur.base != n.base) {
            last = cur
            cur = cur.osrLink
        }

        var next: NMethod? = null
        if (cur != null && cur.base == n.base) {
            found = true
            next = cur.osrLink
            if (last == null) {
                osrNMethodsHead = next
            } else {
                last.osrLink = next
            }
        }
        n.osrLink = null

        return found
    }
}

