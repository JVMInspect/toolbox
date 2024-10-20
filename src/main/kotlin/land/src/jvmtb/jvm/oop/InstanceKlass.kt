package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.*
import land.src.jvmtb.jvm.Address

class InstanceKlass(address: Address) : Klass(address) {
    val majorVersion: Short get() = constantPool.majorVersion
    val minorVersion: Short get() = constantPool.minorVersion
    val constantPool: ConstantPool by struct("_constant_pool")

    val superClass: Klass? by nullableStruct("_super")
    val accessFlags: Int by int("_access_flags")
    val nestHostIndex: Short by short("_nest_host_index")
    val nestMembers: Array<Short> by array("_nest_members")

    val javaFieldsCount: Short get() = TODO()

    val permittedSubclasses: Array<Short> by lazy {
        val nestHostAddress = type.field("_nest_members").offsetOrAddress + pointerSize
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        arrays(permittedSubclassesAddress, false)!!
    }

    val recordComponents: Array<RecordComponent>? by lazy {
        val nestHostAddress = type.field("_nest_members").offsetOrAddress + pointerSize
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        val recordComponentsAddress = permittedSubclassesAddress + pointerSize
        if (unsafe.getAddress(recordComponentsAddress) == 0L)
            return@lazy null
        arrays(recordComponentsAddress, true)
    }

    val sourceDebugExtension: String? by lazy {
        val nestHostAddress = type.field("_nest_members").offsetOrAddress + pointerSize
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        val recordComponentsAddress = permittedSubclassesAddress + pointerSize
        val sourceDebugExtensionAddress = recordComponentsAddress + pointerSize
        if (unsafe.getAddress(sourceDebugExtensionAddress) == 0L)
            return@lazy null
        unsafe.getString(unsafe.getAddress(sourceDebugExtensionAddress))
    }

    val genericSignature: Symbol get() = TODO()
    val sourceFileName: Symbol? by nullableStruct("_source_file_name")

    private val _annotations: Annotations? by nullableStruct("_annotations")

    val annotations: Array<Byte>? get() = _annotations!!.classAnnotations
    val typeAnnotations: Array<Byte>? get() = _annotations!!.classTypeAnnotations
    val fieldsAnnotations: Array<Array<Byte>>? get() = _annotations!!.fieldsAnnotations
    val fieldsTypeAnnotations: Array<Array<Byte>>? get() = _annotations!!.fieldsTypeAnnotations

    val innerClasses: Array<Short>? by nullableArray("_inner_classes")
    val localInterfaces: Array<InstanceKlass>? by nullableArray("_local_interfaces")

    val methods: Array<Method> by array("_methods")
    val _fields: Array<Byte> by array("_fieldinfo_stream")
    val methodOrdering: Array<Int> by array("_method_ordering")
}

