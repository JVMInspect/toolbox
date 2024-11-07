package land.src.toolbox.jvm

import land.src.toolbox.process.ProcessHandle
import land.src.toolbox.process.impl.LinuxProcessHandle
import land.src.toolbox.process.impl.WindowsProcessHandle

fun vm(block: VirtualMachine.() -> Unit) {
    val proc = System.getProperty("os.name").let {
        when {
            it.contains("linux", ignoreCase = true) -> LinuxProcessHandle.current
            it.contains("windows", ignoreCase = true) -> WindowsProcessHandle.current
            else -> error("Unsupported OS: $it")
        }
    }
    vm(proc, block)
}

fun vm(process: ProcessHandle, block: VirtualMachine.() -> Unit) {
    process.attach()

    val vm = VirtualMachine(process)
    vm.block()

    process.detach()
}