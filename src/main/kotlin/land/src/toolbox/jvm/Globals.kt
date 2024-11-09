package land.src.toolbox.jvm

import land.src.toolbox.jvm.oop.CompressedKlassPointers
import land.src.toolbox.jvm.oop.CompressedOops
import land.src.toolbox.jvm.oop.VMSymbols
import land.src.toolbox.jvm.primitive.Address

class Globals(val scope: Scope) {
    val universe = Universe(scope.placeholder)
    val compressedOops = CompressedOops(scope.placeholder)
    val compressedKlassPointers = CompressedKlassPointers(scope.placeholder)
    val vmSymbols = VMSymbols(scope.placeholder)
}