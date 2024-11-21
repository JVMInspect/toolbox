package land.src.toolbox.jvm.oop.gc.g1

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.primitive.Address

class FreeRegionList(address: Address) : HeapRegionSetBase(address) {

    val headOffset: Long = structs.sizeof(HeapRegionSetBase::class).toLong()

    var head: HeapRegion? by maybeNull { offset(headOffset, isPointer = true) }
    var tail: HeapRegion? by maybeNull { offset(headOffset + structs.sizeof(HeapRegion::class), isPointer = true) }

    fun removeFromHead(): HeapRegion {
        val result = head

        length--

        head = result!!.next
        if (head == null) {
            tail = null
        } else {
            head!!.prev = null
        }

        result.next = null

        return result
    }

}