package land.src.toolbox.process.impl

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HMODULE
import land.src.toolbox.process.LibraryHandle
import land.src.toolbox.util.Windows

private val kernel32 = Windows.Kernel32

class WindowsLibraryHandle(
    private var localHandle: HMODULE,
    private var remoteHandle: HMODULE
) : LibraryHandle {
    override fun findProcedure(procedure: String): Pointer? {
        val address = kernel32.GetProcAddress(localHandle, procedure) ?: return null
        val raw = Pointer.nativeValue(address.pointer)
        val ourBase = Pointer.nativeValue(localHandle.pointer)
        val dstBase = Pointer.nativeValue(remoteHandle.pointer)
        val result = raw - ourBase + dstBase
        return Pointer.createConstant(result)
    }
}