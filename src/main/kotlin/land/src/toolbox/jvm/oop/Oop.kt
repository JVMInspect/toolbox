package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

// oop is a oopDesc*
class Oop(address: Address) : Struct(address) {
    val oopDesc: OopDesc by lazy { OopDesc(Address(this, unsafe.getAddress(address.base))) }
}