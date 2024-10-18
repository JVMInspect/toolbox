package land.src.jvmtb.util

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef.HMODULE
import com.sun.jna.platform.win32.WinDef.LPVOID
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.W32APIOptions.ASCII_OPTIONS

val Long.pointer: Pointer get() = Pointer.createConstant(this)

interface Kernel32Ext : Kernel32 {
    @Suppress("FunctionName")
    fun GetProcAddress(hModule: HMODULE, lpProcName: String): LPVOID?

    @Suppress("FunctionName")
    fun VirtualProtectEx(handle: HANDLE, address: Long, size: Long, new: Int, old: IntByReference): LPVOID?

    @Suppress("FunctionName")
    fun QueryFullProcessImageNameA(handle: HANDLE, flags: Int, buffer: ByteArray, length: IntByReference): Boolean
}

object Windows {
    val Kernel32: Kernel32Ext = Native.load("kernel32", Kernel32Ext::class.java, ASCII_OPTIONS)
}