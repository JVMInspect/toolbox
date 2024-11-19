package land.src.toolbox.jvm.oop.gc.shared

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

open class CollectedHeap(address: Address) : Struct(address) {

    val reserved: MemRegion by nonNull("_reserved", isPointer = false)

    fun isIn(address: Long): Boolean = reserved.contains(address)

    open fun allocate(size: Long): Long = TODO()

}