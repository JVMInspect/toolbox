package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class ClassLoaderDataGraph(address: Address) : Struct(address) {
    val head: ClassLoaderData by nonNull("_head")
}