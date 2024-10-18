package land.src.jvmtb.jvm

import land.src.jvmtb.dsl.int
import land.src.jvmtb.dsl.string

class VMVersion(address: Address) : Struct(address) {
    override val type = "Abstract_VM_Version"

    var major by int("_vm_major_version")
    var minor by int("_vm_minor_version")
    var security by int("_vm_security_version")
    var build by int("_vm_build_number")
    var release by string("_s_vm_release", true)

    override fun toString() = release
}