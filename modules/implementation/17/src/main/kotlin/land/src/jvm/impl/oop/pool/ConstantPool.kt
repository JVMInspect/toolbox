package land.src.jvm.impl.oop.pool

import land.src.jvm.api.oop.pool.ConstantPoolCache
import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.maybeNullArray
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.dsl.nonNullArray
import land.src.toolbox.jvm.primitive.*
import land.src.jvm.api.oop.pool.ConstantPool as ConstantPoolApi

class ConstantPool(address: Address) : Struct(address), Oop, ConstantPoolApi {
    override val length: Int by nonNull("_length")
    override val major: Short by nonNull("_major_version")
    override val minor: Short by nonNull("_minor_version")
    override val cache: ConstantPoolCache? by maybeNull("_cache")
    override var tags: NByteArray by nonNullArray("_tags")
    override var operands: NShortArray? by maybeNullArray("_operands")
}