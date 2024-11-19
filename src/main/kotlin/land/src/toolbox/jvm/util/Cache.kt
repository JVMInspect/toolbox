package land.src.toolbox.jvm.util

fun <E, X> makeCache(fn: (E) -> X): (E) -> X {
    val cache = mutableMapOf<E, X>()
    return { e ->
        cache.getOrPut(e) { fn(e) }
    }
}