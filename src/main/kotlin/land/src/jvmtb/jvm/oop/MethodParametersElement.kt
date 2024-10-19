package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct

class MethodParametersElement(address: Address) : Struct(address) {
    val nameCpIndex get() = unsafe.getShort(address.base)
    val flags get() = unsafe.getShort(address.base + 2)
}