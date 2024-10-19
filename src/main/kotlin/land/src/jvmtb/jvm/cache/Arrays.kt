package land.src.jvmtb.jvm.cache

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.oop.Array
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

private val ConstructorType = MethodType
    .methodType(Void.TYPE, KClass::class.java, Boolean::class.java, Address::class.java)

class Arrays(private val scope: VMScope) {
    private val factories = mutableMapOf<KClass<*>, Factory<*>>()

    class Factory<S : Any>(arrayType: KClass<S>, private val elementType: KClass<*>) {
        private val handle = MethodHandles.lookup()
            .findConstructor(arrayType.java, ConstructorType)

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address, isElementPointer: Boolean): S =
            handle(elementType, isElementPointer, address) as S
    }

    inline operator fun <reified E : Any, reified A : Array<E>> invoke(address: Long = -1, isElementPointer: Boolean): A? =
        invoke(E::class, A::class, isElementPointer, address)

    @Suppress("Unchecked_Cast")
    operator fun <E : Any, A : Any> invoke(elementType: KClass<E>, arrayType: KClass<A>, isElementPointer: Boolean, address: Long = -1): A? {
        if (scope.unsafe.getLong(address) == 0L) {
            return null
        }

        val factory = factories.computeIfAbsent(arrayType) { Factory(arrayType, elementType) }
        return factory(Address(scope, address), isElementPointer) as? A
    }
}