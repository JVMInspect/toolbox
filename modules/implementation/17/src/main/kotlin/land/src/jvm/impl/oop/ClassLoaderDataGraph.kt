package land.src.jvm.impl.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.ClassLoaderDataGraph as ClassLoaderDataGraphApi

class ClassLoaderDataGraph(address: Address) : Struct(address), ClassLoaderDataGraphApi {
    override val head: ClassLoaderData by nonNull("_head")
}