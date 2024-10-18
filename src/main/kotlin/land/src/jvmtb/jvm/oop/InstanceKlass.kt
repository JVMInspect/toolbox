package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address

class InstanceKlass(address: Address) : Klass(address) {
    override val type = "InstanceKlass"
}