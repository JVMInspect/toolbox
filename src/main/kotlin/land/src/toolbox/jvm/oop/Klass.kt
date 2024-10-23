package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.cast
import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct

private const val _lh_neutral_value = 0
private const val _lh_array_tag_type_value = 0.inv()
private const val _lh_array_tag_shift = (Integer.BYTES * 8) - 2

open class Klass(address: Address) : Struct(address), Oop {
    enum class Type {
        Klass,
        InstanceKlass,
        ArrayKlass,
    }

    val secondarySupers: Array<land.src.toolbox.jvm.oop.Klass>? by maybeNull("_secondary_supers")

    val name: land.src.toolbox.jvm.oop.Symbol by nonNull("_name")
    val nextLink: land.src.toolbox.jvm.oop.Klass? by maybeNull("_next_link")

    val layoutHelper: Int by nonNull("_layout_helper")

    val accessFlags: Int by nonNull("_access_flags")

    val klassType: land.src.toolbox.jvm.oop.Klass.Type
        get() = when {
        layoutHelper < land.src.toolbox.jvm.oop._lh_neutral_value -> land.src.toolbox.jvm.oop.Klass.Type.InstanceKlass
        layoutHelper > land.src.toolbox.jvm.oop._lh_neutral_value -> land.src.toolbox.jvm.oop.Klass.Type.ArrayKlass
        else -> land.src.toolbox.jvm.oop.Klass.Type.Klass
    }

    val instanceKlass: land.src.toolbox.jvm.oop.InstanceKlass by cast()
}