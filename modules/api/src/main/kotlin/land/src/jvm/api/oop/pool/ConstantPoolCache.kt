package land.src.jvm.api.oop.pool

interface ConstantPoolCache {
    val length: Int

    operator fun get(index: Int): ConstantPoolCacheEntry
}