package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import kotlin.reflect.KProperty

class ConstantImpl<V>(scope: Scope, path: String): Scope by scope {
    @Suppress("Unchecked_Cast")
    private val constant: V by lazy {
        vm.constant(path) as V
    }

    operator fun getValue(struct: Scope, property: KProperty<*>): V = constant
}

fun <V> Scope.constant(path: String) = ConstantImpl<V>(this, path)