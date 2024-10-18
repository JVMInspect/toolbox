package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.struct
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class ClassLoaderData(address: Address) : Struct(address) {
    val klasses: Klass by struct("_klasses")
    val next: ClassLoaderData by struct("_next")
}