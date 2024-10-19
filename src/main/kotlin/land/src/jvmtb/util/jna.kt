package land.src.jvmtb.util

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.linux.LibC
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef.HMODULE
import com.sun.jna.platform.win32.WinDef.LPVOID
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.W32APIOptions.ASCII_OPTIONS

val Long.pointer: Pointer get() = Pointer.createConstant(this)
val Pointer.address: Long get() = Pointer.nativeValue(this)

interface Kernel32Ext : Kernel32 {
    @Suppress("FunctionName")
    fun GetProcAddress(hModule: HMODULE, lpProcName: String): LPVOID?

    @Suppress("FunctionName")
    fun VirtualProtectEx(handle: HANDLE, address: Long, size: Long, new: Int, old: IntByReference): LPVOID?

    @Suppress("FunctionName")
    fun QueryFullProcessImageNameA(handle: HANDLE, flags: Int, buffer: ByteArray, length: IntByReference): Boolean
}

class iovec : Structure() {
    @JvmField
    var iov_base: Pointer? = null
    @JvmField
    var iov_len: Long = 0

    override fun getFieldOrder(): List<String> = listOf("iov_base", "iov_len")
}

fun iovec(base: Pointer, len: Long): iovec = iovec().apply {
    iov_base = base
    iov_len = len
}

open class LinkMap : Structure {
    class LinkMapRef : LinkMap(), ByReference

    constructor() : super()

    constructor(p: Pointer?) : super(p)

    @JvmField
    var l_addr: Long = 0
    @JvmField
    var l_name: String? = null
    @JvmField
    var l_ld: Pointer? = null
    @JvmField
    var l_next: LinkMapRef? = null
    @JvmField
    var l_prev: LinkMapRef? = null

    override fun getFieldOrder(): List<String> = listOf("l_addr", "l_name", "l_ld", "l_next", "l_prev")
}


interface LibCExt : LibC {
    @Suppress("FunctionName")
    fun process_vm_readv(pid: Int, local: Array<iovec>, liovcnt: Int, remote: Array<iovec>, riovcnt: Int, flags: Long): Int

    @Suppress("FunctionName")
    fun process_vm_writev(pid: Int, local: Array<iovec>, liovcnt: Int, remote: Array<iovec>, riovcnt: Int, flags: Long): Int

    fun dlopen(filename: String, flags: Int): LinkMap?

    fun dlclose(handle: Pointer): Int

    fun dlsym(handle: Pointer, symbol: String): Pointer?

    fun ptrace(request: Int, pid: Int, addr: Pointer?, data: Pointer?): Int

    fun waitpid(pid: Int, status: IntByReference, options: Int): Int

    fun __errno_location(): Pointer
}

object Windows {
    val Kernel32: Kernel32Ext = Native.load("kernel32", Kernel32Ext::class.java, ASCII_OPTIONS)
}

object Linux {
    val LibC: LibCExt = Native.load("c", LibCExt::class.java)

    fun errno(): Int {
        val errno = LibC.__errno_location()
        return errno.getInt(0)
    }
}