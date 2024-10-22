package land.src.toolbox.jvm.primitive

import land.src.toolbox.jvm.Scope

class Address(scope: Scope, val base: Long) : Scope by scope {
    companion object {
        const val PLACEHOLDER = -1L
    }
}