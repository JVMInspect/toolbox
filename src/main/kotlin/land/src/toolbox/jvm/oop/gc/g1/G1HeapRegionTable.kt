package land.src.toolbox.jvm.oop.gc.g1

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class G1HeapRegionTable(address: Address) : Struct(address) {

    val dataBase: Long by nonNull("_base")
    val length: Long by nonNull("_length")

    val elementSize: Int by lazy { structs.sizeof(HeapRegion::class) }

    operator fun get(index: Int): HeapRegion? {
        return structs(unsafe.getAddress(dataBase + index * elementSize))
    }

    operator fun iterator(): Iterator<HeapRegion?> {
        return object : Iterator<HeapRegion?> {
            private var index = 0
            override fun hasNext(): Boolean = index < length
            override fun next(): HeapRegion? = get(index++)
        }
    }

}