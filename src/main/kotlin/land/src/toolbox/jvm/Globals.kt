package land.src.toolbox.jvm

import land.src.toolbox.jvm.oop.CompressedKlassPointers
import land.src.toolbox.jvm.oop.CompressedOops
import land.src.toolbox.jvm.primitive.Address

class Globals(val scope: Scope) {
    private val placeholder = Address(scope, Address.PLACEHOLDER)

    val universe = Universe(placeholder)
    val compressedOops = CompressedOops(placeholder)
    val compressedKlassPointers = CompressedKlassPointers(placeholder)
}