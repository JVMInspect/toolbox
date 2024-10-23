package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KProperty

class ConstantImpl<V>(struct: Struct, path: String): Scope by struct {
    @Suppress("Unchecked_Cast")
    private val constant: V by lazy {
        vm.constant(path) as V
    }

    operator fun getValue(struct: Struct, property: KProperty<*>): V = constant
}

fun <V> Struct.constant(path: String) = ConstantImpl<V>(this, path)