package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.cast
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

object MarkForDeoptimizationStatus {
    const val not_marked = 0
    const val deoptimize = 1
    const val deoptimize_noupdate = 2
}

open class CompiledMethod(address: Address) : Struct(address) {
    // _mark_for_deoptimization_status
    val markForDeoptimizationStatus: Int by nonNull {
        offset(0)
    }

    val deoptHandlerBegin: Int by nonNull("_deopt_handler_begin")

    val native: NMethod by cast()
}