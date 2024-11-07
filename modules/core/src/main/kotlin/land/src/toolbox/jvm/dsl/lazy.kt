package land.src.toolbox.jvm.dsl

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

private typealias AddressPropertyPair = Pair<Address, KProperty<*>>

object Recomputable {
    private val LazyMap = mutableMapOf<AddressPropertyPair, RecomputableLazy<*>>()

    operator fun set(property: AddressPropertyPair, lazy: RecomputableLazy<*>) {
        if (LazyMap.containsKey(property))
            return
        LazyMap[property] = lazy
    }

    fun sanity(property: AddressPropertyPair): Boolean
            = LazyMap.containsKey(property)


    fun invalidate(property: AddressPropertyPair) =
        LazyMap[property]?.invalidate()
}

fun <T> invalidate(struct: Struct, property: KProperty0<T>) {
    val address = struct.address
    Recomputable.invalidate(address to property)
}

fun <T> recompute(struct: Struct, property: KProperty0<T>): T {
    val address = struct.address
    Recomputable.invalidate(address to property)
    return property.getValue(struct, property)
}

class RecomputableLazy<T>(private val address: Address, private val initializer: () -> T) {
    private var _value: T? = null

    private val value: T get() {
        if (_value == null) {
            //println("recomputing $this")
            _value = initializer()
        }
        return _value!!
    }

    fun invalidate() {
        //println("invalidating $this")
        _value = null
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        //println("getting ${property.name} from ${address.base} ($this)")
        Recomputable[address to property] = this
        return value
    }
}

fun <T> Struct.recomputableLazy(initializer: () -> T) = RecomputableLazy(address, initializer)

fun <E : Any> Struct.recomputableLazyList(initializer: MutableList<E>.() -> Unit) = recomputableLazy {
    buildList(initializer)
}

fun <K : Any, V : Any> Struct.recomputableLazyMap(initializer: MutableMap<K, V>.() -> Unit) = recomputableLazy {
    buildMap(initializer)
}