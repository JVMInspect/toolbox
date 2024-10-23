package land.src.jvmtb.jvm.oop

import land.src.toolbox.jvm.dsl.cast
import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

private const val _lh_neutral_value = 0
private const val _lh_array_tag_type_value = 0.inv()
private const val _lh_array_tag_shift = (Integer.BYTES * 8) - 2

open class Klass(address: Address) : Struct(address) {
    enum class Type {
        Klass,
        InstanceKlass,
        ArrayKlass,
    }

    val secondarySupers: Array<Klass>? by maybeNull("_secondary_supers")

    val name: Symbol by nonNull("_name")
    val nextLink: Klass? by maybeNull("_next_link")

    val layoutHelper: Int by nonNull("_layout_helper")

    val accessFlags: Int by nonNull("_access_flags")

    val klassType: Type get() = when {
        layoutHelper < _lh_neutral_value -> Type.InstanceKlass
        layoutHelper > _lh_neutral_value -> Type.ArrayKlass
        else -> Type.Klass
    }

    val instanceKlass: InstanceKlass by cast()
}