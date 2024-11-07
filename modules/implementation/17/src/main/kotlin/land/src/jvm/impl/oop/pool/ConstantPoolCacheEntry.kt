package land.src.jvm.impl.oop.pool

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.pool.ConstantPoolCacheEntry as ConstantPoolCacheEntryApi

private const val cp_index_bits = 2 * 8
private const val cp_index_mask = (1 shl cp_index_bits) - 1L

class ConstantPoolCacheEntry(address: Address) : Struct(address), ConstantPoolCacheEntryApi {
    private var indices: Long by nonNull("_indices")

    override val index: Int
        get() = ((indices and 0xffffffffL) and cp_index_mask).toInt()
}