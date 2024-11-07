package land.src.toolbox.process.impl

import com.sun.jna.Memory
import com.sun.jna.ptr.IntByReference
import land.src.toolbox.process.LibraryHandle
import land.src.toolbox.process.ProcessHandle
import land.src.toolbox.process.ProcessHandles
import land.src.toolbox.process.ProcessUnsafe
import land.src.toolbox.util.Linux
import land.src.toolbox.util.address
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

private val LibC = Linux.LibC

class LinuxProcessHandle(override val pid: Int, override val isLocal: Boolean) : ProcessHandle {
    override val unsafe = ProcessUnsafe(this)

    override fun attach() {
        if (pid == current.pid)
            return // we are already attached

        val result = LibC.ptrace(16, pid, null, null)
        check(result != -1) {
            "ptrace attach failed"
        }
        LibC.waitpid(pid, IntByReference(), 0)
    }

    override fun detach() {
        if (pid == current.pid)
            return // no need to detach from self

        val result = LibC.ptrace(17, pid, null, null)
        check(result != -1) {
            "ptrace detach failed"
        }
    }

    private val local = Memory(Long.SIZE_BYTES * 2L)
    private val remote = Memory(Long.SIZE_BYTES * 2L)

    override fun read(src: Long, dst: Long, size: Int): Int {
        local.setLong(0, dst)
        local.setLong(Long.SIZE_BYTES.toLong(), size.toLong())

        remote.setLong(0, src)
        remote.setLong(Long.SIZE_BYTES.toLong(), size.toLong())

        val result = LibC.process_vm_readv(pid, local.address, 1, remote.address, 1, 0)
        check(result != -1) {
            "process_vm_readv failed"
        }

        return result
    }

    override fun write(dst: Long, src: Long, size: Int): Int {
        local.setLong(0, src)
        local.setLong(Long.SIZE_BYTES.toLong(), size.toLong())

        remote.setLong(0, dst)
        remote.setLong(Long.SIZE_BYTES.toLong(), size.toLong())

        val result = LibC.process_vm_writev(pid, local.address, 1, remote.address, 1, 0)
        check(result != -1) {
            "process_vm_writev failed"
        }

        return result
    }

    override fun findLibrary(library: String): LibraryHandle? {
        // iterate over proc maps
        val file = File("/proc/$pid/maps")
        val reader = file.bufferedReader()

        return reader.lineSequence().firstOrNull { it.contains(library) }?.let {
            val path = '/' + it.substringAfter('/')
            val handle = LibC.dlopen(path, 1) ?: return null

            val base = it.substringBefore("-").toLong(16)

            LinuxLibraryHandle(handle, base)
        }
    }

    override fun is64Bit(): Boolean {
        // check if the elf header of the proc exe is 64 bit

        val file = RandomAccessFile("/proc/$pid/exe", "r")
        val channel = file.channel
        // make sure we rewind (procfs keeps seek)
        channel.position(0)
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 4)

        val magic = buffer.int

        channel.close()
        file.close()

        return magic == 0x7f454c46
    }

    companion object : ProcessHandles {
        private val currentPid =
            java.lang.ProcessHandle.current().pid().toInt()

        override val remote: Set<ProcessHandle>
            get() = File("/proc").listFiles()!!
                .asSequence()
                .filter { it.isDirectory }
                .mapNotNull { it.name.toIntOrNull() }
                .filter {
                    // we need to check for java processes
                    val file = File("/proc/$it/cmdline")
                    file.exists() && file.readText().contains("java")
                }
                .map { LinuxProcessHandle(it, it == currentPid) }
                .toSet()

        override val current: ProcessHandle =
            LinuxProcessHandle(currentPid, true)
    }
}