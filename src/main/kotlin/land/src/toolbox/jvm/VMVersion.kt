package land.src.toolbox.jvm

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class VMVersion(address: Address) : Struct(address) {
    override val typeName = "Abstract_VM_Version"

    val major: Int by nonNull("_vm_major_version")
    val minor: Int by nonNull("_vm_minor_version")
    val security: Int by nonNull("_vm_security_version")
    val build: Int by nonNull("_vm_build_number")
    val release: String by nonNull("_s_vm_release", true)

    override fun toString() = release
}