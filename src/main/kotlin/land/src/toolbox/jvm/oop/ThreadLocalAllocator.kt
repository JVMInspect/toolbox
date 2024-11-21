package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class ThreadLocalAllocator(address: Address) : Struct(address) {

    val start: Long by nonNull("_start")
    val end: Long by nonNull("_end")
    var top: Long by nonNull("_top")

    fun allocate(size: Long): Long {
        val newTop = top + size
        if (newTop > end) {
            return 0
        }
        top = newTop
        return top
    }

}