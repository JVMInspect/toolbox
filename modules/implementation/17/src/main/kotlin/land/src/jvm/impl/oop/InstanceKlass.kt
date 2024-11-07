package land.src.jvm.impl.oop

import land.src.jvm.api.oop.Field
import land.src.jvm.impl.oop.pool.ConstantPool
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.nonNullArray
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.NArray
import land.src.jvm.api.oop.InstanceKlass as InstanceKlassApi

//class Bypass(address: Address, elementInfo: ElementInfo) : NArray<Method>(address, elementInfo), NArrayApi<Method>

class InstanceKlass(address: Address) : Klass(address), InstanceKlassApi {
    override val constants: ConstantPool by nonNull("_constants")
    override val fields: NArray<Field>
        get() = TODO("Not yet implemented")
    override val methods: NArray<Method> by nonNullArray("_methods")
}