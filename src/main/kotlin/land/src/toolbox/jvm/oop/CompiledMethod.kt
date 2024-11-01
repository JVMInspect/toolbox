package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

val not_marked = 0
val deoptimize = 1
val deoptimize_noupdate = 2

open class CompiledMethod(address: Address) : Struct(address) {

    /**
     *   enum MarkForDeoptimizationStatus {
     *     not_marked,
     *     deoptimize,
     *     deoptimize_noupdate
     *   };
     *
     */

    // _mark_for_deoptimization_status
    val markForDeoptimizationStatus: Int by nonNull {
        offset(0)
    }

    val deoptHandlerBegin: Int by nonNull("_deopt_handler_begin")

}