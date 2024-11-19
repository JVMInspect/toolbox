package land.src.toolbox.jvm.util

enum class OS {
    LINUX,
    WINDOWS,
    UNSUPPORTED
}

class OSUtil {

    companion object {
        val os by lazy {
            System.getProperty("os.name").let {
                when {
                    it.contains("nux", ignoreCase = true) -> OS.LINUX
                    it.contains("windows", ignoreCase = true) -> OS.WINDOWS
                    else -> OS.UNSUPPORTED
                }
            }
        }
    }

}