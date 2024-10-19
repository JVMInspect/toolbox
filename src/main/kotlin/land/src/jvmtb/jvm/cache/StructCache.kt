package land.src.jvmtb.jvm.cache

import land.src.jvmtb.dsl.mappedTypeName
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.util.isImplementedStruct
import land.src.jvmtb.util.isStruct
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

private val ConstructorType = MethodType
    .methodType(Void.TYPE, Address::class.java)

class StructCache(private val scope: VMScope) : Factory {
    private val sizes = mutableMapOf<KClass<*>, Int>()
    private val names = mutableMapOf<KClass<*>, String>()
    private val factories = mutableMapOf<KClass<*>, Factory<*>>()

    class Factory<S : Any>(type: KClass<S>) {
        init {
            check(type.isStruct) {
                "Cannot create struct factory for non struct type ${type.simpleName}"
            }
            check(type.isImplementedStruct) {
                "Cannot create struct factory for non implemented struct type ${type.simpleName}"
            }
        }

        private val handle = MethodHandles.lookup()
            .findConstructor(type.java, ConstructorType)

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address?): S = handle(address) as S
    }

    inline fun <reified S : Struct> sizeOf(): Int =
        sizeOf(S::class)

    fun nameOf(type: KClass<*>): String =
        names[type] ?: error("${type.simpleName} has not yet been built.")

    fun sizeOf(type: KClass<*>): Int =
        sizes[type] ?: error("${type.simpleName} has not yet been built.")

    inline operator fun <reified S : Struct> invoke(address: Long = -1): S? =
        invoke(S::class, address) as? S

    @Suppress("Parameter_Name_Changed_On_Override")
    override operator fun invoke(structType: KClass<*>, address: Long): Struct? {
        if (address == 0L) {
            return null
        }

        val factory = factories.computeIfAbsent(structType) { Factory(structType) }
        val struct = factory(Address(scope, address)) as Struct
        val name = names.computeIfAbsent(structType) { struct.mappedTypeName }
        val type = scope.vm.type(name)
        val size = sizes.computeIfAbsent(structType) { type.size }

        with (struct) {
            this.type = type
            this.size = size
        }

        return struct
    }
}