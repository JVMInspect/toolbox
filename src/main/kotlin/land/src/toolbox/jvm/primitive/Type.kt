package land.src.toolbox.jvm.primitive

open class Type(
    val name: String,
    val superName: String,
    val size: Int,
    val isOop: Boolean,
    val isInt: Boolean,
    val isUnsigned: Boolean,
    val fields: Set<Field>
) {
    object Undefined : Type("Undefined", "Undefined", -1, false, false, false, emptySet())

    fun field(name: String): Field? =
        fields.firstOrNull { it.name == name }

    override fun toString(): String {
        val sb = StringBuilder(name)
        if (superName.isNotBlank())
            sb.append(" extends ").append(superName)

        sb.append(" @ ").append(size).append('\n')
        for (field in fields) {
            sb.append("  ").append(field).append('\n')
        }

        return sb.toString()
    }
}