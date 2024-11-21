package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.*
import land.src.toolbox.jvm.dsl.offset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import land.src.toolbox.jvm.util.T_NARROWOOP
import land.src.toolbox.jvm.util.T_OBJECT

class ConstantPoolCache(address: Address) : Struct(address), Oop {
    var length: Int by nonNull("_length")

    val dataBase: Long get() =
        address.base + type.size

    val entrySize: Int by lazy {
        structs.sizeof(ConstantPoolCacheEntry::class)
    }

    operator fun get(index: Int): ConstantPoolCacheEntry {
        // todo: sizeof(ConstantPoolCacheEntry)
        return ConstantPoolCacheEntry(Address(this, dataBase + index.toLong() * entrySize))
    }

    val entries: kotlin.Array<ConstantPoolCacheEntry> get() {
        return Array(length) { get(it) }
    }

    //val constantPool: ConstantPool by nonNull("_constant_pool")
    var referenceMap: Array<Short>? by maybeNullArray("_reference_map")
    private val _resolvedReferences: OopHandle? by maybeNull {
        offset(type.field("_constant_pool")!!.offsetOrAddress + pointerSize, isPointer = true)
    }
    val resolvedReferences: ArrayOopDesc? by lazy {
        if (_resolvedReferences == null) null else ArrayOopDesc(_resolvedReferences!!.obj.address)
    }
    //val resolvedFieldEntries: Array<ResolvedFieldEntry> by nonNull("_resolved_field_entries")
    //val resolvedIndyEntries: Array<ResolvedIndyEntry> by nonNull("_resolved_indy_entries")
    //val resolvedMethodEntries: Array<ResolvedMethodEntry> by nonNull("_resolved_method_entries")
    //val resolvedReferences: OopHandle by struct("_resolved_references")

    fun expand(entries: List<ConstantPoolCacheEntry>, references: List<Short>): ConstantPoolCache {
        // TODO FINISH
        val newSize = type.size + ((length + entries.size) * entrySize)
        val newCacheAddress = unsafe.allocateMemory(newSize.toLong())
        val oldMemory = unsafe.getMemory(address.base, type.size + (length * entrySize))
        unsafe.putMemory(newCacheAddress, oldMemory)

        for ((index, entry) in entries.withIndex()) {
            val entryOffset = newCacheAddress + type.size + ((length + index) * entrySize)
            val entryMemory = unsafe.getMemory(entry.base, entrySize)
            unsafe.putMemory(entryOffset, entryMemory)
            println("put $entry at index $index (entry base: $entryOffset, cpIndex: ${entry.cpIndex})")
        }

        // we also need to expand the _reference_map array wit the new references
        val oldReferenceSize = referenceMap?.length ?: 0
        val newReferenceMap: Array<Short> = if (referenceMap == null) {
            arrays.allocate0(references.size)
        } else {
            referenceMap!!.expand(references.size)
        }
        for ((index, reference) in references.withIndex()) {
            newReferenceMap[oldReferenceSize + index] = reference
        }

        // also reallocate the resolved references array
        // a bit more difficult as it's a array oop, we need to reallocate the array and increase its length
        val arrType = if (globals.compressedOops.isCompressedOops()) T_NARROWOOP else T_OBJECT

        val oldLength = resolvedReferences?.length ?: 0
        val newResolvedReferences = if (resolvedReferences == null) {
            objects.allocateArray(arrType, references.size)
        } else {
            resolvedReferences!!.expand(arrType, references.size)
        }
        // just fill it with NULL
        for (i in oldLength until newResolvedReferences.length) {
            newResolvedReferences[i] = if (arrType == T_OBJECT) 0L else 0
        }

        val newCache = ConstantPoolCache(Address(this, newCacheAddress))
        newCache.length = length + entries.size
        newCache.referenceMap = newReferenceMap
        _resolvedReferences!!.obj = newResolvedReferences

        return newCache
    }


}