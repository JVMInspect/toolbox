package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.Scope

fun sizeForType(scope: Scope, type: String): Int {
    return when (type[0]) {
        'B' -> 1
        'C' -> 2
        'D' -> 8
        'F' -> 4
        'I' -> 4
        'J' -> 8
        'S' -> 2
        'Z' -> 1
        '[', 'L' -> scope.globals.compressedOops.objectSize
        else -> throw IllegalArgumentException("Unknown type: $type")
    }
}