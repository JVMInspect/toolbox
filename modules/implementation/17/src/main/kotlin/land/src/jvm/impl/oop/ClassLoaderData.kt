package land.src.jvm.impl.oop

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.ClassLoaderData as ClassLoaderDataApi

class ClassLoaderData(address: Address) : Struct(address), Oop, ClassLoaderDataApi {
    override val klasses: Klass? by maybeNull("_klasses")
    override val next: ClassLoaderData? by maybeNull("_next")

    val allKlasses: Map<String, Klass> get() = buildMap {
        var klass: Klass? = klasses
        while (klass != null) {
            put(klass.name.string, klass)
            klass = klass.nextLink
        }
    }
}