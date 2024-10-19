package land.src.jvmtb.util

import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.jvm.VMScope
import kotlin.reflect.KClass

val KClass<*>.isStruct get() =
    Struct::class.java.isAssignableFrom(java)

val KClass<*>.isImplementedStruct get() =
    isStruct && Struct::class != this

fun VMScope.sizeOf(type: KClass<*>): Int {
    if (type.isStruct)
        return structs.sizeOf(type)

    return when (this) {
        Byte::class -> Byte.SIZE_BYTES
        Short::class -> Short.SIZE_BYTES
        Char::class -> Char.SIZE_BYTES
        Int::class -> Int.SIZE_BYTES
        Long::class -> Long.SIZE_BYTES
        Float::class -> Float.SIZE_BYTES
        Double::class -> Double.SIZE_BYTES
        else -> error("Cannot get size of ${type.simpleName}")
    }
}