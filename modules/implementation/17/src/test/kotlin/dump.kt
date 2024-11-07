import land.src.jvm.impl.Universe
import land.src.toolbox.jvm.vm

fun main() = vm {
    val universe = Universe(this)
    for (loadedKlass in universe.loadedKlasses) {
        println(loadedKlass.key)
    }
}