package land.src.jvmtb.jvm.cache

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.util.isArray
import land.src.jvmtb.util.isStruct
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass
import kotlin.reflect.javaType

private val ConstructorType = MethodType
    .methodType(Void.TYPE, KClass::class.java, Boolean::class.java, Address::class.java)

class Arrays(private val scope: VMScope) {
    private val factories = mutableMapOf<KClass<*>, Factory<*>>()

    class Factory<E : Any>(arrayType: KClass<*>, private val elementType: KClass<E>) {
        private val handle = MethodHandles.lookup()
            .findConstructor(arrayType.java, ConstructorType)

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address, isElementPointer: Boolean): Array<E> =
            handle(elementType, isElementPointer, address) as Array<E>
    }

    inline operator fun <reified E : Any, reified A : Array<E>> invoke(
        address: Long = -1,
        isElementPointer: Boolean
    ): Array<E>? = invoke(E::class, A::class, isElementPointer, address)

    @OptIn(ExperimentalStdlibApi::class)
    fun getElementName(arrayKlass: KClass<*>): String {
        val parameters = arrayKlass.typeParameters[0]
        val elementType = (parameters.upperBounds[0].javaType as Class<*>).kotlin
        val elementName = if (elementType.isStruct) scope.structs.nameOf(elementType) else when (elementType) {
            Byte::class -> "u1"
            Short::class, Char::class -> "u2"
            Int::class -> "int"
            else -> error("No mapped element name for ${elementType.simpleName}")
        }
        return elementName
    }

    @Suppress("Unchecked_Cast")
    operator fun <E : Any> invoke(
        elementType: KClass<E>,
        arrayType: KClass<*>,
        isElementPointer: Boolean,
        address: Long = -1
    ): Array<E>? {
        if (scope.unsafe.getLong(address) == 0L) {
            return null
        }

        check(arrayType.isArray) {
            "Cannot create array for not array type ${arrayType.simpleName}"
        }

        val factory = factories.computeIfAbsent(arrayType) {
            Factory(arrayType, elementType)
        }
        val array = factory(Address(scope, address), isElementPointer) as Array<E>

        val elementName =
            if (elementType.isStruct) scope.structs.nameOf(elementType)
            else if (elementType.isArray) "Array<${getElementName(elementType)}${if (isElementPointer) "*" else ""}>"
            else when (elementType) {
            Byte::class -> "u1"
            Short::class, Char::class -> "u2"
            Int::class -> "int"
            else -> error("No mapped element name for ${elementType.simpleName}")
        }

        val arrayType = scope.vm.type("Array<$elementName${if (!elementType.isArray && isElementPointer) "*" else ""}>")

        with(array) {
            this.type = arrayType
            this.size = arrayType.size
        }

        return array
    }
}