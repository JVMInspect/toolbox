package land.src.jvm.api.oop

import land.src.jvm.api.oop.pool.Symbol

interface Klass {
    enum class Type { Klass, InstanceKlass, ArrayKlass }

    val type: Type
    val access: Int
    val name: Symbol
    val instance: InstanceKlass
}