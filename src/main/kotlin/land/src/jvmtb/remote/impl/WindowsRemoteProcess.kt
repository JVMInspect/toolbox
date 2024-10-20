package land.src.jvmtb.remote.impl

import com.sun.jna.Native.POINTER_SIZE
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.IntByReference
import land.src.jvmtb.remote.RemoteLibrary
import land.src.jvmtb.remote.RemoteProcess
import land.src.jvmtb.remote.RemoteProcessList
import land.src.jvmtb.remote.RemoteUnsafe
import land.src.jvmtb.util.Windows
import java.io.File

private val psapi = Psapi.INSTANCE
private val kernel32 = Windows.Kernel32

private const val MAX_PATH = 260
private const val DONT_RESOLVE_DLL_REFERENCES = 0x1

class WindowsRemoteProcess(private val handle: HANDLE) : RemoteProcess, AutoCloseable {
    override val pid = kernel32.GetProcessId(handle)
    override val unsafe = RemoteUnsafe(this)
    private val libraryHandles = mutableSetOf<HMODULE>()

    fun protect(address: Long, size: Long, protection: Int, block: () -> Unit) {
        val oldProtection = IntByReference()
        kernel32.VirtualProtectEx(handle, address, size, protection, oldProtection)
        block()
        kernel32.VirtualProtectEx(handle, address, size, oldProtection.value, oldProtection)
    }

    override fun read(src: Pointer, dst: Pointer, size: Int): Int {
        val read = IntByReference()
        check(kernel32.ReadProcessMemory(handle, src, dst, size, read)) {
            "ReadProcessMemory"
        }
        return read.value
    }

    override fun write(dst: Pointer, src: Pointer, size: Int): Int {
        val written = IntByReference()
        check(kernel32.WriteProcessMemory(handle, dst, src, size, written)) {
            "WriteProcessMemory"
        }
        return written.value
    }

    override fun findLibrary(library: String): RemoteLibrary? {
        val needed = IntByReference()
        val modules = arrayOfNulls<HMODULE>(512)
        check(psapi.EnumProcessModules(handle, modules, 512 * POINTER_SIZE, needed)) {
            "EnumProcessModules"
        }
        val pathBuf = CharArray(MAX_PATH)

        for (idx in 0..needed.value / POINTER_SIZE) {
            val moduleHandle = modules[idx] ?: break
            val len = psapi.GetModuleFileNameExW(handle, moduleHandle, pathBuf, MAX_PATH)
            check(len != 0) { "GetModuleFileNameExA" }
            val path = String(pathBuf, 0, len)
            val name = File(path).name
            if (name.equals(library)) {
                val lib = kernel32.LoadLibraryEx(path, null, DONT_RESOLVE_DLL_REFERENCES)
                checkNotNull(lib) { "LoadLibraryEx" }
                libraryHandles += lib
                return WindowsRemoteLibrary(lib, moduleHandle)
            }
        }
        return null
    }

    override fun is64Bit(): Boolean {
        val ref = IntByReference()
        check(kernel32.IsWow64Process(handle, ref)) {
            "IsWow64Process"
        }
        return ref.value == 0
    }

    fun closeHandle() {
        check(kernel32.CloseHandle(handle)) {
            "CloseHandle"
        }
    }

    override fun close() {
        for (handle in libraryHandles)
            kernel32.CloseHandle(handle)

        closeHandle()
    }

    companion object : RemoteProcessList {
        override val remotes: Set<RemoteProcess>
            get() = getJavaProcesses()

        val current: RemoteProcess
            get() = WindowsRemoteProcess(kernel32.GetCurrentProcess())

        private fun getActiveProcessIds(): IntArray {
            var size = 0
            var processes: IntArray
            val needed = IntByReference()
            do {
                size += 1024
                processes = IntArray(size)
                if (!psapi.EnumProcesses(processes, size * DWORD.SIZE, needed)) {
                    throw Win32Exception(Kernel32.INSTANCE.GetLastError())
                }
            } while (size == needed.value / DWORD.SIZE)

            val out = IntArray(needed.value / DWORD.SIZE)
            System.arraycopy(processes, 0, out, 0, out.size)
            return out
        }

        fun getJavaProcesses(): Set<RemoteProcess> {
            val processes = mutableSetOf<RemoteProcess>()

            for (processId in getActiveProcessIds()) {
                val handle = kernel32.OpenProcess(0x38, false, processId) ?: continue
                val buffer = ByteArray(MAX_PATH)
                val length = IntByReference(MAX_PATH)
                if(!kernel32.QueryFullProcessImageNameA(handle, 0, buffer, length)) continue

                val path = String(buffer, 0, length.value)
                val name = File(path).name
                if (!name.equals("java.exe") && !name.equals("javaw.exe")) continue
                processes.add(WindowsRemoteProcess(handle))
            }

            return processes
        }
    }
}