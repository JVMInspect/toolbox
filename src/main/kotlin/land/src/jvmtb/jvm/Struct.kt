package land.src.jvmtb.jvm

import land.src.jvmtb.jvm.cache.Arrays
import land.src.jvmtb.jvm.cache.OopCache
import land.src.jvmtb.jvm.cache.StructCache
import land.src.jvmtb.remote.RemoteUnsafe

class Address(val scope: VMScope, val base: Long)

abstract class Struct(val address: Address) : VMScope {
    open var size: Int? = null
    open val typeName: String? = null
    lateinit var type: Type

    override val vm: VirtualMachine get() = address.scope.vm
    override val unsafe: RemoteUnsafe get() = address.scope.unsafe
    override val oops: OopCache get() = address.scope.oops
    override val arrays: Arrays get() = address.scope.arrays
    override val structs: StructCache get() = address.scope.structs
}