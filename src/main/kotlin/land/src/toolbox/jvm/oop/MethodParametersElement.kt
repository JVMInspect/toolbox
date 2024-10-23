package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class MethodParametersElement(address: Address) : Struct(address) {
    val nameCpIndex get() = unsafe.getShort(address.base)
    val flags get() = unsafe.getShort(address.base + 2)
}