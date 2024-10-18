package land.src.jvmtb.jvm

object Arguments {
    private var vmFlagsCount: Long = 0
    private var vmFlags: Long = 0
    private var vmArgsCount: Long = 0
    private var vmArgs: Long = 0
    private var javaCommand: Long = 0

    fun initialize(jvm: VirtualMachine) {
        val type = jvm.type("Arguments")
        vmFlagsCount = type.global("_num_jvm_flags")
        vmFlags = type.global("_jvm_flags_array")
        vmArgsCount = type.global("_num_jvm_args")
        vmArgs = type.global("_jvm_args_array")
        javaCommand = type.global("_java_command")
    }

    fun printArray(jvm: VirtualMachine, countAddress: Long, startAddress: Long) {
        val count = jvm.getInt(countAddress)
        val start = jvm.getAddress(startAddress)
        val stride = if (jvm.is64Bit()) 8 else 4
        for (i in 0 until count) {
            println(jvm.getStringRef(start + (i * stride)))
        }
    }

    fun print(jvm: VirtualMachine) {
        printArray(jvm, vmFlagsCount, vmFlags)
        printArray(jvm, vmArgsCount, vmArgs)
        println(jvm.getStringRef(javaCommand))
    }

    fun commandLine(jvm: VirtualMachine) = jvm.getStringRef(javaCommand)
}