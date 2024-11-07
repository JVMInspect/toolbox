package land.src.jvm.impl.oop

import land.src.toolbox.jvm.dsl.cast
import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.Klass as KlassApi

open class Klass(address: Address) : Struct(address), Oop, KlassApi {
    private val helper: Int by nonNull("_layout_helper")

    override val type: KlassApi.Type get() = when {
        helper < 0 -> KlassApi.Type.InstanceKlass
        helper > 0 -> KlassApi.Type.ArrayKlass
        else -> KlassApi.Type.Klass
    }

    val nextLink: Klass? by maybeNull("_next_link")

    override val name: Symbol by nonNull("_name")
    override val access: Int by nonNull("_access_flags")
    override val instance: InstanceKlass by cast()
}