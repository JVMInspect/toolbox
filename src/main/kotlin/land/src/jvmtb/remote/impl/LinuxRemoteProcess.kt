package land.src.jvmtb.remote.impl

import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import land.src.jvmtb.remote.RemoteLibrary
import land.src.jvmtb.remote.RemoteProcess
import land.src.jvmtb.remote.RemoteProcessList
import land.src.jvmtb.remote.RemoteUnsafe
import land.src.jvmtb.util.Linux
import land.src.jvmtb.util.iovec
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

private val LibC = Linux.LibC

class LinuxRemoteProcess(override val pid: Int) : RemoteProcess {
    override val unsafe = RemoteUnsafe(this)

    override fun attach() {
        val result = LibC.ptrace(16, pid, null, null)
        if (result == -1) {
            error("ptrace attach failed")
        }
        LibC.waitpid(pid, IntByReference(), 0)
    }

    override fun detach() {
        val result = LibC.ptrace(17, pid, null, null)
        if (result == -1) {
            error("ptrace detach failed")
        }
    }

    override fun read(src: Pointer, dst: Pointer, size: Int): Int {
        val local = iovec(dst, size.toLong())
        val remote = iovec(src, size.toLong())

        val result = LibC.process_vm_readv(pid, arrayOf(local), 1, arrayOf(remote), 1, 0)
        if (result == -1) {
            error("process_vm_readv failed")
        }

        return result
    }

    override fun write(dst: Pointer, src: Pointer, size: Int): Int {
        val local = iovec(src, size.toLong())
        val remote = iovec(dst, size.toLong())

        val result = LibC.process_vm_writev(pid, arrayOf(local), 1, arrayOf(remote), 1, 0)
        if (result == -1) {
            error("process_vm_writev failed")
        }

        return result
    }

    override fun findLibrary(library: String): RemoteLibrary? {
        // iterate over proc maps
        val file = File("/proc/$pid/maps")
        val reader = file.bufferedReader()

        return reader.lineSequence().firstOrNull { it.contains(library) }?.let {
            val path = '/' + it.substringAfter('/')
            val handle = LibC.dlopen(path, 1) ?: return null

            val base = it.substringBefore("-").toLong(16)

            LinuxRemoteLibrary(handle, base)
        }
    }

    override fun is64Bit(): Boolean {
        // check if the elf header of the proc exe is 64 bit

        val buffer = ByteBuffer.allocate(4)
        val file = RandomAccessFile("/proc/$pid/exe", "r")
        val channel = file.channel
        channel.read(buffer, 4)
        channel.close()
        file.close()

        return buffer.int == 0x7f454c46
    }

    companion object : RemoteProcessList {
        override val remotes: Set<RemoteProcess>
            get() = File("/proc").listFiles()!!
                .asSequence()
                .filter { it.isDirectory }
                .mapNotNull { it.name.toIntOrNull() }
                .filter {
                    // we need to check for java processes
                    val file = File("/proc/$it/cmdline")
                    file.exists() && file.readText().contains("java")
                }
                .map { LinuxRemoteProcess(it) }
                .toSet()
    }
}