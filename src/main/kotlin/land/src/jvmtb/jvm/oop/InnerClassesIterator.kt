package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.VMScope

class InnerClassInfo(
    var accessFlags: Short,
    var classInfo: Short,
    var innerName: Short,
    var outerClassInfo: Short
)

class InnerClassesIterator(scope: VMScope, ik: InstanceKlass) : VMScope by scope, Iterator<InnerClassInfo> {
    var index = 0L
    val innerClasses = ik.innerClasses
    val length = if (innerClasses != null) innerClasses.length else 0

    private val innerClassAccessFlagsOffset: Long by lazy {
        scope.vm.constant("InstanceKlass::inner_class_access_flags_offset").toLong()
    }

    private val innerClassInnerClassInfoOffset: Long by lazy {
        scope.vm.constant("InstanceKlass::inner_class_inner_class_info_offset").toLong()
    }

    private val innerClassInnerNameOffset: Long by lazy {
        scope.vm.constant("InstanceKlass::inner_class_inner_name_offset").toLong()
    }

    private val innerClassNextOffset: Long by lazy {
        scope.vm.constant("InstanceKlass::inner_class_next_offset").toLong()
    }

    private val innerClassOuterClassInfoOffset: Long by lazy {
        scope.vm.constant("InstanceKlass::inner_class_outer_class_info_offset").toLong()
    }

    override fun hasNext() = length > index

    override fun next(): InnerClassInfo {
        index += innerClassNextOffset
        val access = innerClasses[(index + innerClassAccessFlagsOffset).toInt()]
        val classInfo = innerClasses[(index + innerClassInnerClassInfoOffset).toInt()]
        val className = innerClasses[(index + innerClassInnerNameOffset).toInt()]
        val outerClassInfo = innerClasses[(index + innerClassOuterClassInfoOffset).toInt()]
        return InnerClassInfo(access, classInfo, className, outerClassInfo)
    }
}