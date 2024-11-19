package land.src.toolbox.jvm.oop.gc.g1

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.offset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.toolbox.jvm.util.roundTo

class HeapRegion(address: Address) : Struct(address) {

    val bottom: Long by nonNull("_bottom")
    val end: Long by nonNull("_end")
    var top: Long by nonNull("_top")
    var regionType: Int by nonNull("_type")
    val typeOffset: Long by offset("_type")

    val nextOffset: Long = typeOffset + 4 + pointerSize + 8

    var next: HeapRegion? by maybeNull { offset(nextOffset, isPointer = true) }
    var prev: HeapRegion? by maybeNull { offset(nextOffset + pointerSize, isPointer = true) }
    var containingSet: HeapRegionSetBase? by maybeNull {
        offset(nextOffset + pointerSize * 2, isPointer = true)
    }

    fun isEmpty(): Boolean {
        return top == bottom
    }

    fun isFree(): Boolean {
        return regionType == 0
    }

    fun fits(size: Long): Boolean {
        return (top + size) <= end
    }

}