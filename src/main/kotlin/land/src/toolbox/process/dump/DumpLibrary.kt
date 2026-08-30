package land.src.toolbox.process.dump

import com.sun.jna.Pointer
import net.fornwall.jelf.ElfFile
import net.fornwall.jelf.ElfSegment
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * An ELF shared object on disk, viewed as if it were mapped into the dumped process at [base].
 *
 * Core dumps do not contain clean file backed pages, so everything the JVM keeps in `.text` and
 * `.rodata` — including every type and field name string of the `gHotSpotVMStructs` table — is
 * missing from the dump and has to be served from the original library instead.
 */
class DumpLibrary(override val file: File, override var base: Long = 0) : DumpImage {
    private val raf = RandomAccessFile(file, "r")
    private val channel: FileChannel = raf.channel

    val elf: ElfFile = ElfFile.from(file)

    private val programHeaders = (0 until elf.e_phnum).map { elf.getProgramHeader(it) }

    val loads = programHeaders
        .filter { it.p_type == ElfSegment.PT_LOAD }
        .sortedBy { it.p_vaddr }

    override val segments = loads.map { ImageSegment(it.p_vaddr, it.p_memsz, it.p_flags) }

    override val start get() = base + (loads.firstOrNull()?.p_vaddr ?: 0)
    override val end get() = base + (loads.maxOfOrNull { it.p_vaddr + it.p_memsz } ?: 0)

    private val byteOrder =
        if (elf.ei_data == ElfFile.DATA_MSB) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN

    private val symbols: Map<String, Long> by lazy {
        val result = HashMap<String, Long>()

        // .dynsym first so a full .symtab, present when the build has not been stripped, wins for
        // symbols that appear in both.
        for (section in listOfNotNull(elf.dynamicSymbolTableSection, elf.symbolTableSection)) {
            for (symbol in section.symbols) {
                val name = runCatching { symbol.name }.getOrNull() ?: continue
                if (name.isEmpty() || symbol.st_value == 0L) continue
                result[name] = symbol.st_value
            }
        }

        result
    }

    /** The GNU build id, which identifies this exact build of the library. */
    override val signature: ByteArray? by lazy {
        programHeaders
            .filter { it.p_type == ElfSegment.PT_NOTE }
            .flatMap { parseNotes(readFile(it.p_offset, it.p_filesz.toInt()), byteOrder) }
            .firstOrNull { it.type == NT_GNU_BUILD_ID && it.name == "GNU" }
            ?.description
    }

    override fun findProcedure(procedure: String): Pointer? {
        val value = symbols[procedure] ?: return null
        return Pointer.createConstant(base + value)
    }

    override fun read(address: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        val offset = address - base

        val segment = loads.firstOrNull { offset >= it.p_vaddr && offset < it.p_vaddr + it.p_memsz }
            ?: return 0

        val offsetInSegment = offset - segment.p_vaddr
        val wanted = minOf(length.toLong(), segment.p_vaddr + segment.p_memsz - offset).toInt()

        // Anything past p_filesz is bss, which the file does not store but which is zero at load.
        val fromFile = minOf(wanted.toLong(), maxOf(0, segment.p_filesz - offsetInSegment)).toInt()

        if (fromFile > 0) {
            val bytes = readFile(segment.p_offset + offsetInSegment, fromFile)
            System.arraycopy(bytes, 0, destination, destinationOffset, fromFile)
        }
        if (fromFile < wanted) {
            destination.fill(0, destinationOffset + fromFile, destinationOffset + wanted)
        }

        return wanted
    }

    private fun readFile(offset: Long, length: Int): ByteArray {
        val buffer = ByteBuffer.allocate(length)

        var position = offset
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer, position)
            if (read <= 0) break
            position += read
        }

        return buffer.array()
    }

    override fun close() = raf.close()

    override fun toString() = "${file.name} @ 0x${base.toString(16)}"
}
