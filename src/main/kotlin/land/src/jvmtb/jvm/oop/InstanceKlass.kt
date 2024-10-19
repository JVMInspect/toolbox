package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.*
import land.src.jvmtb.jvm.Address

class InstanceKlass(address: Address) : Klass(address) {
    val constantPool: ConstantPool by struct("_constant_pool")
    val majorVersion: Short get() = constantPool.majorVersion
    val minorVersion: Short get() = constantPool.minorVersion

    val accessFlags: Int by int("_access_flags")

    val nestHostIndex: Short by short("_nest_host_index")
    val nestMembers: Array<Short> by array("_nest_members")

    val permittedSubclasses: Array<Short> by lazy {
        val nestHostAddress = type.field("_nest_members").offsetOrAddress + pointerSize
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        arrays<Short, Array<Short>>(permittedSubclassesAddress, false)!!
    }

    val recordComponents: Array<Short>? by lazy {
        val nestHostAddress = type.field("_nest_members").offsetOrAddress + pointerSize
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        val recordComponentsAddress = permittedSubclassesAddress + pointerSize
        if (unsafe.getAddress(recordComponentsAddress) == 0L)
            return@lazy null
        arrays<Short, Array<Short>>(recordComponentsAddress, false)
    }

    val genericSignature: Symbol get() = TODO()
    val sourceFileName: Symbol? by nullableStruct("_source_file_name")
    val sourceDebugExtension: Symbol? by nullableStruct("_source_debug_extension")

    private val _annotations: Annotations? by nullableStruct("_annotations")

    val annotations: Array<Byte>? get() = _annotations!!.classAnnotations
    val typeAnnotations: Array<Byte>? get() = _annotations!!.classTypeAnnotations

    val innerClasses: Array<Short> by array("_inner_classes")

    val localInterfaces: Array<InstanceKlass>? by nullableArray("_local_interfaces")

    val methods: Array<Method> by array("_methods")
    val methodOrdering: Array<Int> by array("_method_ordering")
}

