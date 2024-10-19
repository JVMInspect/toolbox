package land.src.jvmtb.jvm.cache

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.VMScope
import land.src.jvmtb.jvm.oop.Array
import land.src.jvmtb.jvm.oop.Klass
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

private val ConstructorType = MethodType
    .methodType(Void.TYPE, KClass::class.java, Boolean::class.java, Address::class.java)

class Arrays(private val scope: VMScope) {
    private val factories = mutableMapOf<KClass<*>, Factory<*>>()

    class Factory<E : Any>(arrayType: KClass<out Array<E>>, private val elementType: KClass<E>) {
        private val handle = MethodHandles.lookup()
            .findConstructor(arrayType.java, ConstructorType)

        @Suppress("Unchecked_Cast")
        operator fun invoke(address: Address, isElementPointer: Boolean): Array<E> =
            handle(elementType, isElementPointer, address) as Array<E>
    }

    inline operator fun <reified E : Any, reified A : Array<E>> invoke(
        address: Long = -1,
        isElementPointer: Boolean
    ): Array<E> = invoke(E::class, A::class, isElementPointer, address)

    @Suppress("Unchecked_Cast")
    operator fun <E : Any> invoke(
        elementType: KClass<E>,
        arrayType: KClass<out Array<E>>,
        isElementPointer: Boolean,
        address: Long = -1
    ): Array<E> {
        val factory = factories.computeIfAbsent(arrayType) { Factory(arrayType, elementType) }
        return factory(Address(scope, address), isElementPointer) as Array<E>

        //if (scope.unsafe.getLong(address) == 0L) {
        //    return null
        //}
//
        //val isPrimitive =
        //    elementType == Byte::class ||
        //            elementType == Short::class ||
        //            elementType == Char::class ||
        //            elementType == Int::class
//
        //val arrayElementType = ArrayElementType(
        //    isArray = elementType.isArray,
        //    isOop = elementType.isOop,
        //    isStruct = elementType.isStruct,
        //    isPrimitive = isPrimitive,
        //    isPointer = isElementPointer,
        //    oopType = if (elementType.isOop) elementType as KClass<out Oop> else null,
        //    structType = if (elementType.isStruct) elementType as KClass<out Struct> else null,
        //    arrayType = if (elementType.isArray) elementType as KClass<out Array<*>> else null,
        //    elementType = if (elementType.isArray) elementType as KClass<out Struct> else null,
        //    primitiveType = if (isPrimitive) elementType else null
        //)
//
        //val factory = factories.computeIfAbsent(arrayType) { Factory(arrayType, elementType) }
        //val elementName = if (elementType.isStruct) scope.structs.nameOf(elementType) else when (elementType) {
        //    Byte::class -> "u1"
        //    Short::class, Char::class -> "u2"
        //    Int::class -> "int"
        //    else -> error("No mapped element name for ${elementType.simpleName}")
        //}
        //val arrayType = scope.vm.type("Array<$elementName${if (isElementPointer) "*" else ""}>")
        //val array = factory(Address(scope, address), isElementPointer) as? A
        //(array as Array<E>).apply {
        //    type = arrayType
        //}
        //return array
    }
}