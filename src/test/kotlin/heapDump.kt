import land.src.toolbox.jvm.HeapDump
import land.src.toolbox.jvm.util.DumpMode
import land.src.toolbox.jvm.util.KlassDumper
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Opens a JVM dump offline, walks its universe, and optionally reconstitutes classes out of it.
 *
 * ```
 * heapDump <core-or-minidump> [libjvm.so|jvm.dll] [libjvm-base-in-hex]
 *          [--scan] [--no-recorded-files] [--classes=<filter>] [--out=<dir>] [--minimal]
 * ```
 *
 * `--scan` ignores the dump's module list and locates the library by sweeping the dump for its
 * image, which is the path a dump without a module list has to take. `--no-recorded-files` refuses
 * to read anything the dump left out from the files it named, leaving only the dump itself and the
 * libraries passed in here.
 *
 * `--classes=<filter>` runs every loaded class whose name contains the filter through
 * [KlassDumper] — the port of HotSpot's own class file reconstituter — and writes the result to
 * `--out` (default `./dumped-classes`). Pass `--classes=` to take everything. Reconstituting a
 * class needs its constant pool and method metadata, all of which lives in dumped writable memory,
 * so this works on a dump exactly as it does on a live process.
 *
 * A usable dump is `gcore <pid>` on Linux, or `procdump -ma <pid>` on Windows.
 */
@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("--") }
    val positional = args.filterNot { it.startsWith("--") }

    fun flag(name: String) = flags.any { it == "--$name" }
    fun option(name: String) = flags.firstOrNull { it.startsWith("--$name=") }?.substringAfter('=')

    if (positional.isEmpty()) {
        println("usage: heapDump <dump> [libjvm] [base] [--scan] [--no-recorded-files] " +
            "[--classes=<filter>] [--out=<dir>] [--minimal]")
        return
    }

    val heapDump = HeapDump.open {
        dump = File(positional[0])
        libjvm = positional.getOrNull(1)?.let(::File)
        libjvmBase = positional.getOrNull(2)?.removePrefix("0x")?.toLong(16)
        forceScan = flag("scan")
        useRecordedFiles = !flag("no-recorded-files")
    }

    heapDump.use {
        println("dump:     ${heapDump.source}")
        println("images:   ${heapDump.resolved.joinToString()}")
        println("recorded: ${heapDump.mapped.size} mapped files")
        println("version:  ${heapDump.vm.version} (${if (heapDump.vm.is64Bit) 64 else 32} bit)")

        val universe = heapDump.universe

        println("heap:     ${universe.collectedHeap}")

        val klasses = universe.loadedKlasses
        println("klasses:  ${klasses.size}")

        val filter = option("classes")

        if (filter == null) {
            klasses.keys.sorted().take(10).forEach { println("  $it") }

            val string = universe.instanceKlass("java/lang/String")
            println("String:   ${string?.address?.base?.toHexString()}")
            string?.methods?.take(5)?.forEach { println("  ${it.name}${it.signature}") }

            return@use
        }

        val out = File(option("out") ?: "dumped-classes")
        val mode = if (flag("minimal")) DumpMode.Minimal else DumpMode.Full

        // Array klasses have no class file, and the classes the VM spun up for lambdas were never
        // one to begin with.
        val selected = klasses
            .filterKeys { it.contains(filter) && !it.startsWith("[") && !it.contains("$\$Lambda") }
            .toSortedMap()

        println("dumping:  ${selected.size} classes to ${out.path} ($mode)")

        var written = 0
        val failures = mutableListOf<String>()

        for ((name, klass) in selected) {
            val instanceKlass = klass.instanceKlass ?: continue
            val file = File(out, "$name.class")

            val result = runCatching {
                file.parentFile.mkdirs()
                DataOutputStream(FileOutputStream(file)).use { stream ->
                    KlassDumper(heapDump.vm, instanceKlass, stream, mode).writeClassFileFormat()
                }
            }

            if (result.isSuccess) {
                written++
            } else {
                file.delete()
                failures += "$name: ${result.exceptionOrNull()?.message}"
            }
        }

        println("written:  $written")
        if (failures.isNotEmpty()) {
            println("failed:   ${failures.size}")
            failures.take(10).forEach { println("  $it") }
        }
    }
}
