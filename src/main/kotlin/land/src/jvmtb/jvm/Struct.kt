package land.src.jvmtb.jvm

class Address(val scope: VMScope, val base: Long)

abstract class Struct(val address: Address) {
    open val type: String? = null
}