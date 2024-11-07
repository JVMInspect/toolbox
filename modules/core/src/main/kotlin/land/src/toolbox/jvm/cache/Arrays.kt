package land.src.toolbox.jvm.cache

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.NArray
import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.NByteArray
import land.src.toolbox.jvm.primitive.NShortArray
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

private val ConstructorType = MethodType.methodType(
    Void.TYPE,
    Address::class.java,
    NArray.ElementInfo::class.java
)

typealias ArrayAndElementTypes = Pair<KClass<*>, NArray.ElementInfo>

class Arrays(scope: Scope) : Scope by scope {
    private val cache = mutableMapOf<Long, NArray<*>>()
    private val elements = mutableMapOf<KClass<*>, KClass<*>>()
    private val factories = mutableMapOf<ArrayAndElementTypes, Factory<*>>()

    private class Factory<A : NArray<*>>(arrayType: KClass<*>, private val elementInfo: NArray.ElementInfo) {
        private val handle = MethodHandles.lookup()
            .findConstructor(arrayType.java, ConstructorType)

        init {
            require(arrayType != NArray::class || elementInfo.type != NArray::class) {
                "Use a primitive array such as ByteArray for the element type of nested arrays."
            }
        }

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address): A = handle(address, elementInfo) as A
    }

    private fun elementSize(elementType: KClass<*>): Int = when (elementType) {
        Byte::class -> Byte.SIZE_BYTES
        Short::class, Char::class -> Short.SIZE_BYTES
        Int::class -> Int.SIZE_BYTES
        // we assume this is a pointer
        else -> 8
    }

    private fun elementOffset(elementType: KClass<*>): Int =
        if (structs.isStruct(elementType)) 8 else 4

     fun isArray(type: KClass<*>): Boolean =
        NArray::class.java.isAssignableFrom(type.java)

    fun getElementType(arrayType: KClass<*>): KClass<*> = elements.computeIfAbsent(arrayType) {
        when (arrayType) {
            NByteArray::class -> Byte::class
            NShortArray::class -> Short::class
            else -> error("No array element type mapped for ${arrayType.simpleName}")
        }
    }

    private fun <E : Any> ElementInfo(elementType: KClass<E>, isPointer: Boolean): NArray.ElementInfo = NArray.ElementInfo(
        type = elementType,
        size = elementSize(elementType),
        offset = elementOffset(elementType),
        isArray = isArray(elementType),
        // if the element type is a pointer, this will refer to the element type of the embedded array
        isPointer = isPointer
    )

    operator fun invoke(
        address: Long,
        arrayType: KClass<*>,
        elementType: KClass<*>,
        isElementPointer: Boolean,
    ): NArray<*>? {
        if (address == 0L)
            return null

        if (cache.containsKey(address))
            return cache[address] as NArray<*>

        val info = ElementInfo(elementType, isElementPointer)
        val factory = factories.computeIfAbsent(arrayType to info) {
            Factory<NArray<*>>(arrayType, info)
        }

        val array = factory(Address(this, address))
        cache[address] = array

        return array
    }

    @Suppress("Unchecked_Cast")
    inline operator fun <reified E : Any, reified A : NArray<E>> invoke(
        address: Long,
        isElementPointer: Boolean
    ): NArray<E>? = this(address, A::class, E::class, isElementPointer) as? A?
}