package land.src.toolbox.jvm.primitive

import land.src.toolbox.jvm.Scope

/**
 * The base class for the HotSpot structs
 */
abstract class Struct(val address: Address) : Scope by address {
    val type: Type by lazy {
        vm.type(typeName ?: this::class.simpleName ?: "No type name")
    }

    open val typeName: String? = null
}