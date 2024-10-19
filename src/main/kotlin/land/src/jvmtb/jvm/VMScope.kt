package land.src.jvmtb.jvm

import land.src.jvmtb.jvm.cache.Arrays
import land.src.jvmtb.jvm.cache.OopCache
import land.src.jvmtb.jvm.cache.StructCache
import land.src.jvmtb.remote.RemoteUnsafe

interface VMScope {
    val vm: VirtualMachine
    val unsafe: RemoteUnsafe
    val oops: OopCache
    val arrays: Arrays
    val structs: StructCache
}