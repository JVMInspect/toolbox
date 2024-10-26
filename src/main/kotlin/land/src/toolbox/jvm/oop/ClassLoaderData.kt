package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct

class ClassLoaderData(address: Address) : Struct(address), Oop {
    val loader: OopHandle? by maybeNull("_class_loader")
    val klasses: Klass? by maybeNull("_klasses")
    val next: ClassLoaderData? by maybeNull("_next")
}