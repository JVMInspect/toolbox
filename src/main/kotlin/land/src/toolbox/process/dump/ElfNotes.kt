package land.src.toolbox.process.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** `n_type` of the GNU build id note, the most reliable way to tell two builds of a library apart. */
const val NT_GNU_BUILD_ID = 3

private fun align4(value: Int) = (value + 3) and 3.inv()

/**
 * Parses the note entries packed into a `PT_NOTE` segment.
 *
 * Used both for the notes of a core dump and for the notes of an ordinary shared object, so it has
 * to survive truncated input: parsing stops at the first entry that does not fit.
 */
fun parseNotes(bytes: ByteArray, order: ByteOrder): List<DumpNote> {
    val data = ByteBuffer.wrap(bytes).order(order)
    val result = mutableListOf<DumpNote>()

    while (data.remaining() >= 12) {
        val nameSize = data.int
        val descriptionSize = data.int
        val type = data.int

        if (nameSize < 0 || descriptionSize < 0) break
        if (data.remaining() < align4(nameSize)) break

        val nameBytes = ByteArray(nameSize)
        data.get(nameBytes)
        data.position(data.position() + align4(nameSize) - nameSize)

        if (data.remaining() < descriptionSize) break

        val description = ByteArray(descriptionSize)
        data.get(description)
        data.position(minOf(data.limit(), data.position() + align4(descriptionSize) - descriptionSize))

        result += DumpNote(String(nameBytes).trimEnd('\u0000', ' '), type, description)
    }

    return result
}
