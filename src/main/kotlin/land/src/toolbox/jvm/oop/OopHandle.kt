package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class OopHandle(address: Address) : Struct(address) {
    // _obj is an oop*, and oop is a oopDesc*
    var obj: OopDesc by nonNull("_obj")

    companion object {
        fun allocate(scope: Scope, obj: OopDesc): OopHandle {
            val address = scope.unsafe.allocateMemory(8)
            return OopHandle(Address(scope, address)).apply {
                this.obj = obj
            }
        }
    }
}