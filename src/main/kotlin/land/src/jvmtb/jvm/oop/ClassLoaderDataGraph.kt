package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.struct
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class ClassLoaderDataGraph(address: Address) : Struct(address) {
    val head: ClassLoaderData by struct("_head")

    override val type = "ClassLoaderDataGraph"
}