package land.src.toolbox.jvm.cache

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Oop
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

private val ConstructorType = MethodType.methodType(
    Void.TYPE,
    Address::class.java
)

class Structs(scope: Scope) : Scope by scope {
    private val factories = mutableMapOf<KClass<*>, Factory<*>>()

    private class Factory<S : Struct>(structType: KClass<*>) {
        private val handle = MethodHandles.lookup()
            .findConstructor(structType.java, ConstructorType)

        init {
            check(structType != Struct::class && structType != Oop::class) {
                "Factory must create implemented types"
            }
        }

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address): S = handle(address) as S
    }

    fun isStruct(type: KClass<*>): Boolean =
        Struct::class.java.isAssignableFrom(type.java)

    operator fun invoke(address: Long, structType: KClass<*>): Struct? {
        if (address == 0L)
            return null

        check(!arrays.isArray(structType)) {
            "Tried to use structs(...) on Array type"
        }

        val factory = factories.computeIfAbsent(structType) {
            Factory<Struct>(structType)
        }

        return factory(Address(this, address))
    }

    inline operator fun <reified S : Struct> invoke(address: Long): S? =
        this(address, S::class) as S?
}