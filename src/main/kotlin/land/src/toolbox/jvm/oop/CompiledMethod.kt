package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.cast
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

open class CompiledMethod(address: Address) : Struct(address) {
    val deoptHandlerBegin: Int by nonNull("_deopt_handler_begin")

    val nmethod: nmethod by cast()

}