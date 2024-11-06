package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.*
import land.src.toolbox.jvm.dsl.offset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct

class ConstantPoolCache(address: Address) : Struct(address), Oop {
    val length: Int by nonNull("_length")

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
    val referenceMap: Array<Short>? by maybeNullArray("_reference_map")
    private val _resolvedReferences: OopHandle? by maybeNull {
        offset(type.field("_constant_pool")!!.offsetOrAddress + pointerSize)
    }
    val resolvedReferences: ArrayOopDesc? by lazy {
        if (_resolvedReferences == null) null else ArrayOopDesc(_resolvedReferences!!.obj.address)
    }
    //val resolvedFieldEntries: Array<ResolvedFieldEntry> by nonNull("_resolved_field_entries")
    //val resolvedIndyEntries: Array<ResolvedIndyEntry> by nonNull("_resolved_indy_entries")
    //val resolvedMethodEntries: Array<ResolvedMethodEntry> by nonNull("_resolved_method_entries")
    //val resolvedReferences: OopHandle by struct("_resolved_references")


}