package land.src.jvmtb.jvm

class Type(
    val name: String,
    val superName: String,
    val size: Int,
    val isOop: Boolean,
    val isInt: Boolean,
    val isUnsigned: Boolean,
    val fields: Set<Field>
) {
    fun field(name: String) =
        fields.firstOrNull { it.name == name } ?: throw NoSuchFieldException(name)

    fun global(name: String): Long {
        val field = field(name)
        if (field.isStatic)
            return field.offsetOrAddress

        throw IllegalArgumentException("${this.name}.$name is not a static field")
    }

    fun offset(name: String): Long {
        val field = field(name)
        if (!field.isStatic)
            return field.offsetOrAddress

        throw IllegalArgumentException("${this.name}.$name is not an instance field")
    }
}