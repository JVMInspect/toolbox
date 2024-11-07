package land.src.jvm.api.oop

import land.src.jvm.api.NArray
import land.src.jvm.api.oop.pool.ConstantPool

interface InstanceKlass {
    val constants: ConstantPool
    val fields: NArray<Field>
    val methods: NArray<Method>
}