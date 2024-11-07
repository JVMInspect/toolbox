package land.src.toolbox.jvm.primitive

import land.src.jvm.api.Addressable
import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.dsl.*

/**
 * The base class for the HotSpot structs.
 *
 * To declare named fields, use the [nonNull], [maybeNull], [nonNullArray] or [maybeNullArray] delegates.
 *
 * To declare un-named fields and those which have varying offsets between versions, use the delegates
 * with a [FieldLocationProviderScope] to provide the offset manually.
 *
 * In the case that a field is only exported in specific versions, use the version components:
 * - [FieldLocationProviderScope.major]
 * - [FieldLocationProviderScope.minor]
 * - [FieldLocationProviderScope.build]
 *
 * To selectively choose an [FieldLocationProviderScope.offset] or [FieldLocationProviderScope.address] to use in place of the [FieldLocationProviderScope.name].
 */
abstract class Struct(val address: Address) : Scope by address, Addressable by address {
    val vmType: Type by lazy {
        vm.type(vmTypeName ?: this::class.simpleName ?: error("No type name provided"))
    }

    open val vmTypeName: String? = null
}