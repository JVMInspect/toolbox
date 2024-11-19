package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

const val cp_index_bits = 2 * 8
const val cp_index_mask = (1 shl cp_index_bits) - 1L

class ConstantPoolCacheEntry(address: Address) : Struct(address) {
    var indices: Long by nonNull("_indices")
    var f1: Long by nonNull("_f1")
    var f2: Long by nonNull("_f2")
    var flags: Long by nonNull("_flags")

    val cpIndex: Int by lazy {
        ((indices and 0xffffffffL) and cp_index_mask).toInt()
    }

    fun setCpIndex(index: Int) {
        indices = index.toLong() and 0xffffffffL
    }
}