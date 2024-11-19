package land.src.toolbox.jvm.oop.gc.g1

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.offset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class HeapRegionManager(address: Address) : Struct(address) {

    val regions: G1HeapRegionTable by nonNull("_regions", isPointer = false)
    val regionsOffset: Long by offset("_regions")
    val freeList: FreeRegionList by nonNull {
        offset(regionsOffset + structs.sizeof(G1HeapRegionTable::class).toLong() + pointerSize * 3, isPointer = false)
    }

}