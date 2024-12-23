package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.dsl.constant

class InnerClassInfo(
    var accessFlags: Short,
    var classInfo: Short,
    var innerName: Short,
    var outerClassInfo: Short
)

class InnerClassesIterator(scope: Scope, ik: InstanceKlass) : Scope by scope, Iterator<InnerClassInfo> {
    private var index = 0
    private val innerClasses = ik.innerClasses
    var size = innerClasses?.length ?: 0
    val entries get() = size / innerClassNextOffset

    private val innerClassAccessFlagsOffset: Int by constant("InstanceKlass::inner_class_access_flags_offset")
    private val innerClassInnerClassInfoOffset: Int by constant("InstanceKlass::inner_class_inner_class_info_offset")
    private val innerClassInnerNameOffset: Int by constant("InstanceKlass::inner_class_inner_name_offset")
    private val innerClassNextOffset: Int by constant("InstanceKlass::inner_class_next_offset")
    private val innerClassOuterClassInfoOffset: Int by constant("InstanceKlass::inner_class_outer_class_info_offset")
    private val innerClassEnclosingMethodAttributeSize: Int by constant("InstanceKlass::enclosing_method_attribute_size")

    init {
        if (size % innerClassNextOffset == innerClassEnclosingMethodAttributeSize)
            size -= innerClassEnclosingMethodAttributeSize
    }

    override fun hasNext() = index < size

    override fun next(): InnerClassInfo {
        val array = innerClasses!!
        val access = array[index + innerClassAccessFlagsOffset]!!
        val classInfo = array[index + innerClassInnerClassInfoOffset]!!
        val className = array[index + innerClassInnerNameOffset]!!
        val outerClassInfo = array[index + innerClassOuterClassInfoOffset]!!

        index += innerClassNextOffset

        return InnerClassInfo(access, classInfo, className, outerClassInfo)
    }
}