package land.src.jvmtb.util

import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.jvm.oop.Oop
import kotlin.reflect.KClass

val KClass<*>.isStruct get() =
    Struct::class.java.isAssignableFrom(java)

val KClass<*>.isArray get() =
    Array::class.java.isAssignableFrom(java)

val KClass<*>.isOop get() =
    Oop::class.java.isAssignableFrom(java)


val KClass<*>.isImplementedStruct get() =
    isStruct && Struct::class != this

val KClass<*>.structName get() =
    simpleName

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