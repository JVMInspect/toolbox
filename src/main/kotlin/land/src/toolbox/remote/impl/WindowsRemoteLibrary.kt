package land.src.toolbox.remote.impl

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HMODULE
import land.src.toolbox.remote.RemoteLibrary
import land.src.toolbox.util.Windows

private val kernel32 = Windows.Kernel32

class WindowsRemoteLibrary(
    private var localHandle: HMODULE,
    private var remoteHandle: HMODULE
) : RemoteLibrary {
    override fun findProcedure(procedure: String): Pointer? {
        val address = kernel32.GetProcAddress(localHandle, procedure) ?: return null
        val raw = Pointer.nativeValue(address.pointer)
        val ourBase = Pointer.nativeValue(localHandle.pointer)
        val dstBase = Pointer.nativeValue(remoteHandle.pointer)
        val result = raw - ourBase + dstBase
        return Pointer.createConstant(result)
    }
}