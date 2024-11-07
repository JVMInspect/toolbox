package land.src.jvm.api.oop

import land.src.jvm.api.Addressable

fun interface Constructor<D, S> {
    fun construct(address: Addressable, struct: D): S
}