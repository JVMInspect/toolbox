package land.src.toolbox.jvm.cache

import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.ByteArray
import land.src.toolbox.jvm.primitive.ShortArray
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

private val ConstructorType = MethodType.methodType(
    Void.TYPE,
    Address::class.java,
    Array.ElementInfo::class.java
)

typealias ArrayAndElementTypes = Pair<KClass<*>, Array.ElementInfo>

class Arrays(scope: Scope) : Scope by scope {
    private val cache = mutableMapOf<Long, Array<*>>()
    private val elements = mutableMapOf<KClass<*>, KClass<*>>()
    val factories = mutableMapOf<ArrayAndElementTypes, Factory<*>>()

    class Factory<A : Array<*>>(arrayType: KClass<*>, private val elementInfo: Array.ElementInfo) {
        private val handle = MethodHandles.lookup()
            .findConstructor(arrayType.java, ConstructorType)

        init {
            require(arrayType != Array::class || elementInfo.type != Array::class) {
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
        Array::class.java.isAssignableFrom(type.java)

    fun getElementType(arrayType: KClass<*>): KClass<*> = elements.computeIfAbsent(arrayType) {
        when (arrayType) {
            ByteArray::class -> Byte::class
            ShortArray::class -> Short::class
            else -> error("No array element type mapped for ${arrayType.simpleName}")
        }
    }

    fun <E : Any> ElementInfo(elementType: KClass<E>, isPointer: Boolean): Array.ElementInfo = Array.ElementInfo(
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
    ): Array<*>? {
        if (address == 0L)
            return null

        if (cache.containsKey(address))
            return cache[address] as Array<*>

        val info = ElementInfo(elementType, isElementPointer)
        val factory = factories.computeIfAbsent(arrayType to info) {
            Factory<Array<*>>(arrayType, info)
        }

        val array = factory(Address(this, address))
        cache[address] = array

        return array
    }

    @Suppress("Unchecked_Cast")
    inline operator fun <reified E : Any, reified A : Array<E>> invoke(
        address: Long,
        isElementPointer: Boolean
    ): Array<E>? = this(address, A::class, E::class, isElementPointer) as? Array<E>?

    fun allocate0(
        length: Int,
        arrayType: KClass<*>,
        elementType: KClass<*>,
        isElementPointer: Boolean
    ): Array<*> {
        val info = ElementInfo(elementType, isElementPointer)

        val sizeOfHeader = info.offset + info.size
        val sizeOfContent = length * info.size
        val address = unsafe.allocateMemory((sizeOfHeader + sizeOfContent).toLong())

        val factory = factories.computeIfAbsent(arrayType to info) {
            Factory<Array<*>>(arrayType, info)
        }

        return factory(Address(this, address))
    }

    inline fun <reified E : Any, reified A : Array<E>> allocate(length: Int, isElementPointer: Boolean = false): Array<E> =
        allocate0(length, E::class, A::class, isElementPointer) as A
}