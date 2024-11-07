package land.src.jvm.impl.oop

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.jvm.api.oop.Method as MethodApi

class Method(address: Address) : Struct(address), MethodApi {
    override val name: String
        get() = TODO("Not yet implemented")
    override val signature: String
        get() = TODO("Not yet implemented")
}