package land.src.jvmtb.jvm.cache

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.Struct
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

private val ConstructorType = MethodType
    .methodType(Void.TYPE, Address::class.java)

private fun isStruct(type: KClass<*>) =
    Struct::class.java.isAssignableFrom(type.java)

private fun isImplementedStruct(type: KClass<*>) =
    Struct::class != type

class StructCache(val scope: VMScope) : Factory {
    val map = mutableMapOf<KClass<*>, Factory<*>>()

    class Factory<S : Any>(type: KClass<S>) {
        init {
            check(isStruct(type)) {
                "Cannot create struct factory for non struct type ${type.simpleName}"
            }
            check(isImplementedStruct(type)) {
                "Cannot create struct factory for non implemented struct type ${type.simpleName}"
            }
        }

        private val handle = MethodHandles.lookup()
            .findConstructor(type.java, ConstructorType)

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address): S = handle(address) as S
    }

    inline operator fun <reified S : Struct> invoke(address: Long = -1): S =
        map.computeIfAbsent(S::class) { Factory(S::class) }(Address(scope, address)) as S

    override operator fun invoke(type: KClass<*>, address: Long): Any =
        map.computeIfAbsent(type) { Factory(type) }(Address(scope, address))
}