package land.src.jvmtb.jvm

import land.src.jvmtb.dsl.int
import land.src.jvmtb.dsl.string

class VMVersion(address: Address) : Struct(address) {
    override val typeName = "Abstract_VM_Version"

    val major by int("_vm_major_version")
    val minor by int("_vm_minor_version")
    val security by int("_vm_security_version")
    val build by int("_vm_build_number")
    val release by string("_s_vm_release", true)

    override fun toString() = release
}