package land.src.jvmtb.jvm.cache

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.oop.Array
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

private val ConstructorType = MethodType
    .methodType(Void.TYPE, KClass::class.java, Address::class.java)

class Arrays(private val scope: VMScope) {
    private val factories = mutableMapOf<KClass<*>, Factory<*>>()

    class Factory<S : Any>(arrayType: KClass<S>, private val elementType: KClass<*>) {
        private val handle = MethodHandles.lookup()
            .findConstructor(arrayType.java, ConstructorType)

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address): S = handle(elementType, address) as S
    }

    inline operator fun <reified E : Any, reified A : Array<E>> invoke(address: Long = -1): A =
        invoke(E::class, A::class, address)

    @Suppress("Unchecked_Cast")
    operator fun <E : Any, A : Array<E>> invoke(elementType: KClass<E>, arrayType: KClass<A>, address: Long = -1): A {
        val factory = factories.computeIfAbsent(arrayType) { Factory(arrayType, elementType) }
        val name = scope.structs.nameOf(elementType)
        println("element name is $name")
        //val name = names.computeIfAbsent(structType) { struct.mappedTypeName }
        //val type = scope.vm.type(name)
        //val size = sizes.computeIfAbsent(structType) { type.size }
        return factory(Address(scope, address)) as A
    }
}