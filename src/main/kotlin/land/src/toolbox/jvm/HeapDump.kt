package land.src.toolbox.jvm

import land.src.toolbox.process.dump.CoreDump
import land.src.toolbox.process.dump.DumpImage
import land.src.toolbox.process.dump.DumpImageScanner
import land.src.toolbox.process.dump.DumpLibrary
import land.src.toolbox.process.dump.DumpSource
import land.src.toolbox.process.dump.MappedFileImage
import land.src.toolbox.process.dump.MiniDump
import land.src.toolbox.process.dump.PeLibrary
import land.src.toolbox.process.impl.HeapDumpProcessHandle
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/**
 * How the load address of a library inside the dump was determined. Useful to report, because a
 * wrong base produces confusing failures much later, when a struct field reads as garbage.
 */
enum class BaseResolution {
    /** The caller supplied the address outright. */
    SUPPLIED,

    /** Read from the module list the operating system wrote into the dump. */
    MODULE_LIST,

    /** Recovered by sweeping the dump for the image header and matching its build identity. */
    SCANNED
}

/** A library placed into the dumped address space, and how its address was arrived at. */
class ResolvedImage(val image: DumpImage, val resolution: BaseResolution) {
    override fun toString() =
        "${image.file.name} @ 0x${image.base.toString(16)} ($resolution)"
}

/**
 * Opens a JVM dump — a Linux ELF core dump or a Windows minidump — as a [VirtualMachine], so that
 * the whole [Universe] can be walked offline.
 *
 * A dump alone is not enough. It preserves the pages HotSpot wrote to, which is where the VM
 * globals and the `gHotSpotVMStructs` entries live, but not the pages it only ever read, which is
 * where the symbol table and every type and field name string live. Supplying `libjvm.so` (or
 * `jvm.dll`) closes that gap: the file is positioned at the address the dump says it was loaded at,
 * and reads that miss the dump are served from it.
 *
 * ```
 * HeapDump.open {
 *     dump = File("core.12345")
 *     libjvm = File("/usr/lib/jvm/jdk-21/lib/server/libjvm.so")
 * }.use { heapDump ->
 *     println(heapDump.universe.loadedKlasses.keys)
 * }
 * ```
 */
class HeapDump private constructor(
    val source: DumpSource,
    val resolved: List<ResolvedImage>,
    /**
     * Files the dump recorded as mapped and that still exist on this machine, served raw. Comes
     * after [resolved] so an explicitly supplied library always wins for its own range.
     */
    val mapped: List<DumpImage>,
    structsFile: File?
) : Closeable {

    val process = HeapDumpProcessHandle(source, resolved.map { it.image } + mapped)

    /** The library the VM structs are read out of. */
    val libjvm = resolved.first().image

    val vm: VirtualMachine
    val universe: Universe get() = vm.universe

    init {
        check(libjvm.findProcedure("gHotSpotVMStructs") != null) {
            "${libjvm.file.name} does not export gHotSpotVMStructs — it is either stripped of its " +
                "dynamic symbols or not the JVM library"
        }

        vm = VirtualMachine(process, structsFile)
    }

    override fun close() = process.close()

    override fun toString() = "HeapDump($source, ${resolved.joinToString()})"

    /** Configuration for [HeapDump.open]. */
    class Builder {
        /** The core dump or minidump to read. Required. */
        var dump: File? = null

        /**
         * The `libjvm.so` / `jvm.dll` matching the dump. Optional only when the dump's module list
         * names a path that still exists on this machine.
         */
        var libjvm: File? = null

        /**
         * Address `libjvm` was loaded at. Leave unset to take it from the dump's module list, or,
         * failing that, to recover it by scanning the dump for the image.
         */
        var libjvmBase: Long? = null

        /** An offline dump of the VM structs, for when the table cannot be read from the dump. */
        var vmStructs: File? = null

        /**
         * Ignore the module list and locate every library by scanning the dump instead. For dumps
         * whose module list is missing or wrong — and to check the scanner against a dump that has
         * both.
         */
        var forceScan: Boolean = false

        /**
         * Serve mappings the dump omitted from the files it recorded, when those still exist at the
         * recorded path. This is what makes the CDS archive readable; turn it off to work strictly
         * from the dump and the libraries passed in here.
         */
        var useRecordedFiles: Boolean = true

        internal val extra = mutableListOf<Pair<File, Long?>>()

        /** Adds another library, so reads into its range can fall back to the file on disk. */
        fun library(file: File, base: Long? = null) {
            extra += file to base
        }
    }

    companion object {
        /** Opens [dump], placing [libjvm] at [libjvmBase] or at the address the dump reports. */
        fun open(
            dump: File,
            libjvm: File? = null,
            libjvmBase: Long? = null,
            vmStructs: File? = null
        ): HeapDump = open {
            this.dump = dump
            this.libjvm = libjvm
            this.libjvmBase = libjvmBase
            this.vmStructs = vmStructs
        }

        fun open(block: Builder.() -> Unit): HeapDump {
            val builder = Builder().apply(block)

            val dumpFile = requireNotNull(builder.dump) { "No dump file supplied" }
            require(dumpFile.isFile) { "${dumpFile.path} does not exist" }

            val source = openSource(dumpFile)
            val opened = mutableListOf<DumpImage>()

            try {
                val scanner = DumpImageScanner(source)

                val libjvmFile = builder.libjvm ?: locateLibjvm(source)
                    ?: error(
                        "No libjvm supplied and ${dumpFile.name} does not name one that exists on " +
                            "this machine. Pass the libjvm.so or jvm.dll the dump was taken with."
                    )
                require(libjvmFile.isFile) { "${libjvmFile.path} does not exist" }

                val resolved = mutableListOf(resolve(libjvmFile, builder.libjvmBase, scanner, builder.forceScan))
                for ((file, base) in builder.extra) {
                    resolved += resolve(file, base, scanner, builder.forceScan)
                }
                opened += resolved.map { it.image }

                val mapped =
                    if (builder.useRecordedFiles) recordedFiles(source, resolved.map { it.image })
                    else emptyList()
                opened += mapped

                return HeapDump(source, resolved, mapped, builder.vmStructs)
            } catch (error: Throwable) {
                opened.forEach { runCatching { it.close() } }
                source.close()
                throw error
            }
        }

        /** Opens a dump file, picking the reader by magic rather than by file name. */
        fun openSource(file: File): DumpSource =
            if (MiniDump.isMiniDump(file)) MiniDump(file) else CoreDump(file)

        /** Opens a library file, picking the reader by magic rather than by file name. */
        fun openImage(file: File, base: Long = 0): DumpImage =
            if (isPe(file)) PeLibrary(file, base) else DumpLibrary(file, base)

        private fun resolve(
            file: File,
            suppliedBase: Long?,
            scanner: DumpImageScanner,
            forceScan: Boolean
        ): ResolvedImage {
            val image = openImage(file)

            try {
                suppliedBase?.let {
                    image.base = it
                    return ResolvedImage(image, BaseResolution.SUPPLIED)
                }

                if (!forceScan) scanner.findByModuleList(stemOf(file))?.let {
                    image.base = it
                    return ResolvedImage(image, BaseResolution.MODULE_LIST)
                }

                scanner.scanFor(image)?.let {
                    image.base = it
                    return ResolvedImage(image, BaseResolution.SCANNED)
                }
            } catch (error: Throwable) {
                image.close()
                throw error
            }

            image.close()
            error(
                "Could not find ${file.name} in the dump: it is not in the module list and no " +
                    "mapped image matches this build. Pass its load address explicitly."
            )
        }

        /**
         * Wraps every file the dump recorded as mapped and that is still readable at that path, so
         * ranges the dump left out can be read from it.
         *
         * On Windows the equivalent is done through [PeLibrary], because a PE image is laid out
         * differently in memory than in the file and cannot be read as a flat mapping.
         */
        private fun recordedFiles(source: DumpSource, explicit: List<DumpImage>): List<DumpImage> {
            val covered = explicit.map { it.file.absolutePath }.toSet()

            val fromMappings = source.fileMappings
                .filter { it.path.startsWith("/") && !it.path.endsWith("(deleted)") }
                .groupBy { it.path }
                .mapNotNull { (path, ranges) ->
                    val file = File(path)
                    if (!file.isFile || file.absolutePath in covered) return@mapNotNull null

                    runCatching { MappedFileImage(file, ranges) as DumpImage }.getOrNull()
                }

            if (fromMappings.isNotEmpty()) return fromMappings

            return source.modules.mapNotNull { module ->
                val file = File(module.path)
                if (!file.isFile || file.absolutePath in covered) return@mapNotNull null

                runCatching { openImage(file, module.base) }.getOrNull()
            }
        }

        private fun locateLibjvm(source: DumpSource): File? =
            source.modules
                .filter { it.name.contains("jvm", ignoreCase = true) }
                .map { File(it.path) }
                .firstOrNull { it.isFile }

        private fun stemOf(file: File) = file.name
            .removeSuffix(".so").removeSuffix(".dll").removeSuffix(".dylib")
            .removePrefix("lib")

        private fun isPe(file: File): Boolean = runCatching {
            RandomAccessFile(file, "r").use {
                it.readByte() == 'M'.code.toByte() && it.readByte() == 'Z'.code.toByte()
            }
        }.getOrDefault(false)
    }
}
