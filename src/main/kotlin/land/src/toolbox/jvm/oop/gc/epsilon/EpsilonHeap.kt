package land.src.toolbox.jvm.oop.gc.epsilon

import land.src.toolbox.jvm.dsl.maybeNull
import land.src.toolbox.jvm.oop.gc.shared.CollectedHeap
import land.src.toolbox.jvm.oop.gc.shared.ContiguousSpace
import land.src.toolbox.jvm.primitive.Address

class EpsilonHeap(address: Address): CollectedHeap(address) {

    val space: ContiguousSpace? by maybeNull("_space")

}