package land.src.toolbox.jvm

import land.src.toolbox.jvm.primitive.Field
import land.src.toolbox.jvm.primitive.Type

class VMStructs(private val types: MutableMap<String, Type> = mutableMapOf()) {
    fun print(): String {
        val sb = StringBuilder()
        for (type in types.values) {
            sb.append(type).append('\n')
        }
        return sb.toString()
    }

    fun parse(structs: String) {
        val lines = structs.lines()
        var i = 0

        while (i < lines.size) {
            val header = lines[i++]
            if (header.isBlank()) continue
            val (namesRaw, size) = header.split(" @ ")
            val names = if (namesRaw.contains(" extends ")) {
                val (name, superName) = namesRaw.split(" extends ")
                name to superName
            } else {
                namesRaw to ""
            }

            val fields = mutableSetOf<Field>()
            while (i < lines.size) {
                val line = lines[i++]
                if (line.isBlank()) break
                var split = line.substring(2).split(" ")
                val isStatic = split[0] == "static"
                if (isStatic)
                    split = split.drop(1)

                var typeName: String
                if (split.size != 4) {
                    // take everything from the beginning until length is 3
                    typeName = split.take(split.size - 3).joinToString(" ")
                    split = split.drop(split.size - 3)
                } else {
                    typeName = split[0]
                    split = split.drop(1)
                }

                val (fieldName, _, offsetOrAddress) = split
                val offset =
                    if (isStatic)
                        offsetOrAddress.substring(2).toLong(16)
                    else
                        offsetOrAddress.toLong(16)
                fields.add(Field(fieldName, typeName, offset, isStatic))
            }

            val (name, superName) = names

            types[name] = Type(name, superName, size.toInt(), false, false, false, fields.toSet())
        }
    }

    operator fun get(name: String) = types[name]

}