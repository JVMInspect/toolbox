package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.util.isStruct
import kotlin.reflect.KClass

private fun <T : Any> VMScope.getTypeName(elementType: KClass<T>): String {
    if (elementType.isStruct)
        return structs.nameOf(elementType)

    return when (elementType) {
        Byte::class -> "u1"
        Short::class, Char::class -> "u2"
        Int::class -> "int"
        else -> error("Cannot get element ${elementType.simpleName} from array.")
    }
}

class Array<E : Any>(private val elementType: KClass<E>, address: Address) : Struct(address) {
    val length: Int get() = TODO()
    val bytes: ByteArray get() = TODO()

    operator fun get(index: Int): E {
        TODO()
    }
}