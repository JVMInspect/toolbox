package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class ClassLoaderData(address: Address) : Struct(address) {
    val klasses: land.src.toolbox.jvm.oop.Klass? by maybeNull("_klasses")
    val next: ClassLoaderData? by maybeNull("_next")
}