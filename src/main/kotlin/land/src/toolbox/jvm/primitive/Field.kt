package land.src.toolbox.jvm.primitive

class Field(
    val name: String,
    val typeName: String,
    val offsetOrAddress: Long,
    val isStatic: Boolean
) : Comparable<Field> {
    override fun compareTo(other: Field): Int {
        if (isStatic != other.isStatic)
            return if (isStatic) -1 else 1

        return offsetOrAddress.compareTo(other.offsetOrAddress)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        val offsetHex = offsetOrAddress.toHexString()

        return if (isStatic) "static $typeName $name @0x$offsetHex"
        else "$typeName $name @ $offsetHex"
    }
}