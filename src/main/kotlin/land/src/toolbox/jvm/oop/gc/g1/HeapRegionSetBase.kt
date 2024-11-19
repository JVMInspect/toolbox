package land.src.toolbox.jvm.oop.gc.g1

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

open class HeapRegionSetBase(address: Address) : Struct(address) {

    var length: Int by nonNull("_length")

}