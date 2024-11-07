package land.src.toolbox.jvm

import land.src.jvm.api.Addressable

val Addressable.scope get() =
    this as? Scope ?: error("${this::class.qualifiedName} does not implement Scope")