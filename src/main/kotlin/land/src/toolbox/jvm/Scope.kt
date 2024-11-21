package land.src.toolbox.jvm

import land.src.toolbox.jvm.cache.Arrays
import land.src.toolbox.jvm.cache.Fields
import land.src.toolbox.jvm.cache.Oops
import land.src.toolbox.jvm.cache.Structs
import land.src.toolbox.jvm.oop.JavaThread
import land.src.toolbox.jvm.oop.Thread
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.util.DynamicTypeResolver
import land.src.toolbox.process.ProcessUnsafe

interface Scope {

    val placeholder: Address
        get() = Address(this, Address.PLACEHOLDER)

    /**
     * Access to oops, use `oops<T>(address)` to access oops from cache.
     */
    val oops: Oops

    /**
     * Access to arrays, use `arrays<ElementT, ArrayT>(address)` to access arrays from cache.
     */
    val arrays: Arrays

    /**
     * Access to oops, use `structs<T>(address)` to access structs.
     */
    val structs: Structs

    /**
     * Access to cache for fields of structs (used internally).
     */
    val structFields: Fields

    /**
     * The base JVM instance for this scope.
     */
    val vm: VirtualMachine

    /**
     * Globals, such as Structs with primarily static fields.
     */
    val globals: Globals

    /**
     * Access to the VM's version information.
     */
    val version: VMVersion

    /**
     * Direct access to the VM's encompassing process space.
     */
    val unsafe: ProcessUnsafe

    /**
     * The universe
     */
    val universe: Universe

    /**
     * The classes
     */
    val vmClasses: VMClasses

    /**
     * Object allocation
     */
    val objects: Objects

    /**
     * Native process symbol lookup
     */
    val symbols: Symbols

    /**
     * Dynamic type resolution
     */
    val dynamicTypeResolver: DynamicTypeResolver

    val thread: JavaThread get() = Thread.current(this)

    val pointerSize: Long get() = if (vm.is64Bit) 8 else 4
}