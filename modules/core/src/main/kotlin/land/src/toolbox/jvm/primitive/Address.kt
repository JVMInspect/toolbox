package land.src.toolbox.jvm.primitive

import land.src.jvm.api.Addressable
import land.src.toolbox.jvm.Scope

class Address(scope: Scope, override var base: Long) : Addressable, Scope by scope {
    companion object {
        const val PLACEHOLDER = -1L
    }
}