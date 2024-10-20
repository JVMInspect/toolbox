package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.*
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

private const val _lh_neutral_value = 0
private const val _lh_array_tag_type_value = 0.inv()
private const val _lh_array_tag_shift = (Integer.BYTES * 8) - 2

open class Klass(address: Address) : Struct(address) {
    enum class Type {
        Klass,
        InstanceKlass,
        ArrayKlass,
    }

    // field Klass _secondary_supers Array<Klass*>* false 40 0x0

    val secondarySupers: Array<Klass>? by nullableArray("_secondary_supers")

    val name: Symbol by oop("_name")
    val nextLink: Klass? by nullableStruct("_next_link")

    val layoutHelper by int("_layout_helper")

    val accessFlags: Int by int("_access_flags")

    val klassType: Type get() = when {
        layoutHelper < _lh_neutral_value -> Type.InstanceKlass
        layoutHelper > _lh_neutral_value -> Type.ArrayKlass
        else -> Type.Klass
    }

    val instanceKlass: InstanceKlass get() = InstanceKlass(address)
}