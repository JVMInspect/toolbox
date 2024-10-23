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
            .findConstructor(structType.java, ConstructorType)!!

        init {
            require(structType != Struct::class && structType != Oop::class) {
                "Factory must create implemented types (${structType.simpleName} is a raw type)"
            }
        }

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address): S = handle(address) as S
    }

    fun isStruct(type: Class<*>): Boolean =
        Struct::class.java.isAssignableFrom(type)

    fun isStruct(type: KClass<*>): Boolean =
        isStruct(type.java)

    operator fun invoke(address: Long, structType: KClass<*>): Struct? {
        if (address == 0L)
            return null

        require(!arrays.isArray(structType)) {
            "Use Scope#arrays, Struct#nonNullArray or Struct#maybeNullArray to declare arrays"
        }

        val factory = factories.computeIfAbsent(structType) {
            Factory<Struct>(structType)
        }

        return factory(Address(this, address))
    }

    inline operator fun <reified S : Struct> invoke(address: Long): S? =
        this(address, S::class) as S?
}