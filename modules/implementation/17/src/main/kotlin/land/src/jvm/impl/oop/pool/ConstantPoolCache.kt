package land.src.jvm.impl.oop.pool

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.pool.ConstantPoolCache as ConstantPoolCacheApi

class ConstantPoolCache(address: Address) : Struct(address), Oop, ConstantPoolCacheApi {
    override val length: Int by nonNull("_length")

    private val dataBase: Long get() =
        address.base + vmType.size

    private val entrySize: Int by lazy {
        structs.sizeof(ConstantPoolCacheEntry::class)
    }

    override fun get(index: Int): ConstantPoolCacheEntry {
        return ConstantPoolCacheEntry(Address(this, dataBase + index.toLong() * entrySize))
    }
}