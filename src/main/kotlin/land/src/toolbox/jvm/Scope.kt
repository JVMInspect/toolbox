package land.src.toolbox.jvm

import land.src.toolbox.jvm.cache.Arrays
import land.src.toolbox.jvm.cache.Fields
import land.src.toolbox.jvm.cache.Oops
import land.src.toolbox.jvm.cache.Structs
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.remote.RemoteUnsafe

interface Scope {
    val oops: Oops
    val arrays: Arrays
    val structs: Structs
    val structFields: Fields
    val vm: VirtualMachine
    val version: VMVersion
    val unsafe: RemoteUnsafe

    val pointerSize: Long get() = if (vm.is64Bit()) 8 else 4
    val static: Address get() = Address(this, -1L)
}