package land.src.toolbox.jvm.util

fun maskBits(x: Int, m: Int): Int {
    return x and m
}

fun roundTo(x: Int, s: Int): Int {
    val m = s - 1
    return maskBits(x + m, m.inv())
}
