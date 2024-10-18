package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.oop
import land.src.jvmtb.dsl.struct
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

    val name: Symbol by oop("_name")
    val nextLink: Klass by struct("_next_link")
}