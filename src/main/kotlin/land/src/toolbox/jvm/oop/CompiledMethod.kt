package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.cast
import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Oop
import land.src.toolbox.jvm.primitive.Struct

object MarkForDeoptimizationStatus {
    const val not_marked = 0
    const val deoptimize = 1
    const val deoptimize_noupdate = 2
}

open class CompiledMethod(address: Address) : Struct(address), Oop {
    // _mark_for_deoptimization_status
    var markForDeoptimizationStatus: Int by nonNull {
        offset(0)
    }

    val method: Method? by maybeNull("_method")
    val deoptHandlerBegin: Long by nonNull("_deopt_handler_begin")
    val deoptHandlerEnd: Long by nonNull("_deopt_handler_end")

    val native: NMethod by cast()
}