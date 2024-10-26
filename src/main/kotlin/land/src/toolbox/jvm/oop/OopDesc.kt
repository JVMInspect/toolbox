package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.address
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class OopDesc(address: Address) : Struct(address) {
    override val typeName: String = "oopDesc"

    val _klass: Klass by nonNull("_metadata._klass")

    var useCompressedKlassPointers = true

    val klass: Klass get() {
        if (!useCompressedKlassPointers)
            return _klass

        val narrowKlass = _klass.base
        val narrowKlassBase = globals.compressedKlassPointers.narrowKlassBase
        val narrowKlassShift = globals.compressedKlassPointers.narrowKlassShift
        val klass = narrowKlassBase + (narrowKlass shl narrowKlassShift)

        return structs<Klass>(klass)!!
     }
}