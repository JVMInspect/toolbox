package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.*
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

private const val _lh_array_tag_type_value = 0.inv()
private const val _lh_array_tag_shift = (Integer.BYTES * 8) - 2

open class Klass(address: Address) : Struct(address) {
    enum class Type {
        InstanceKlass,
        ArrayKlass,
        TypeArrayKlass,
        ObjArrayKlass
    }

    // field Klass _secondary_supers Array<Klass*>* false 40 0x0

    val secondarySupers: Array<Klass>? by nullableArray("_secondary_supers")

    val name: Symbol by oop("_name")
    val nextLink: Klass? by nullableStruct("_next_link")

    val instanceKlass: InstanceKlass get() = InstanceKlass(address)
}