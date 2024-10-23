package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class CastDelegate<S : Struct>(
    val struct: Struct,
    val type: KClass<S>,
    offset: Long
) : Scope by struct {
    interface Factory<V> {
        operator fun invoke(): V?
    }

    class StructFactory<S : Any>(
        scope: Scope,
        private val address: Long,
        private val structType: KClass<S>
    ) : Scope by scope, Factory<S> {
        @Suppress("Unchecked_Cast")
        override fun invoke(): S? = structs(address, structType) as? S?
    }

    class OopFactory<O : Any>(
        scope: Scope,
        private val address: Long,
        private val oopType: KClass<O>
    ) : Scope by scope, Factory<O> {
        @Suppress("Unchecked_Cast")
        override fun invoke(): O? = oops(address, oopType) as? O?
    }

    private val factory: Factory<S>? by lazy {
        val address = struct.address.base + offset
        if (oops.isOop(type))
            return@lazy OopFactory<S>(this, address, type)
        if (structs.isStruct(type))
            return@lazy StructFactory<S>(this, address, type)
        else null
    }

    operator fun getValue(thisRef: Struct, property: KProperty<*>): S {
        return factory!!() as S
    }
}

inline fun <reified S : Struct> Struct.cast(offset: Long = 0) =
    CastDelegate(this, S::class, offset)