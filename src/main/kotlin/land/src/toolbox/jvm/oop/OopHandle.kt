package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class OopHandle(address: Address) : Struct(address) {
    // _obj is an oop*, and oop is a oopDesc*
    var obj: OopDesc by nonNull("_obj")
}