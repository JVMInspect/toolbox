package land.src.toolbox.process

interface ProcessHandles {
    val current: ProcessHandle
    val remote: Set<ProcessHandle>
}