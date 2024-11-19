package land.src.toolbox.jvm.oop.gc.shared

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class MemRegion(address: Address) : Struct(address) {

    val start: Long by nonNull("_start")
    val wordSize: Long by nonNull("_word_size")

    val end: Long get() = start + wordSize

    fun contains(address: Long): Boolean = address >= start && address < end

}