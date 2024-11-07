package land.src.toolbox.jvm

import land.src.jvm.api.Addressable
import kotlin.reflect.KClass

class Globals(scope: Scope) : Scope by scope {
    fun isAddressable(type: KClass<*>) =
        Addressable::class.java.isAssignableFrom(type.java)
}