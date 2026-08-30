package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.oop.OopDesc
import land.src.toolbox.jvm.oop.Symbol
import land.src.toolbox.jvm.primitive.Address
import org.objectweb.asm.ClassWriter
import sun.misc.Unsafe
import java.util.Objects
import kotlin.random.Random

val localUnsafe = {
    val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
    theUnsafe.isAccessible = true

    theUnsafe.get(null) as Unsafe
}()

fun toVMObject(scope: Scope, obj: Object): OopDesc {
    val array = arrayOf(obj)
    // access the first element of the array
    val base = localUnsafe.arrayBaseOffset(array.javaClass)

    if (scope.globals.compressedOops.isCompressedOops()) {
        val narrowOopBase = scope.globals.compressedOops.narrowOopBase
        val narrowOopShift = scope.globals.compressedOops.narrowOopShift

        val oop = (localUnsafe.getInt(array, base.toLong()).toLong()) and 0xFFFFFFFFL

        return OopDesc(Address(scope, ((oop shl narrowOopShift) + narrowOopBase)))
    } else {
        return OopDesc(Address(scope, localUnsafe.getLong(array, base.toLong())))
    }
}

fun toRealObject(scope: Scope, oop: OopDesc): Object {
    val address = oop.address.base

    val array = arrayOf(Object())
    val base = localUnsafe.arrayBaseOffset(array.javaClass)

    if (scope.globals.compressedOops.isCompressedOops()) {
        val narrowOopBase = scope.globals.compressedOops.narrowOopBase
        val narrowOopShift = scope.globals.compressedOops.narrowOopShift

        localUnsafe.putInt(array, base.toLong(), ((address - narrowOopBase) shr narrowOopShift).toInt())
    } else {
        localUnsafe.putLong(array, base.toLong(), address)
    }

    return array[0]
}

internal class DeleteClassLoader : ClassLoader() {
    fun define(name: String, bytes: ByteArray): Class<*> {
        return defineClass(name, bytes, 0, bytes.size)
    }

    fun define(bytes: ByteArray): Class<*> {
        return defineClass(bytes, 0, bytes.size)
    }
}

fun loadLocalSymbols(scope: Scope, symbols: Collection<String>): List<Symbol> {
    val classWriter = ClassWriter(0)
    val name = "f$" + Random(System.currentTimeMillis()).nextLong().toHex()
    classWriter.visit(52, 0, name, null, "java/lang/Object", null)
    symbols.forEach { symbol ->
        classWriter.newConst(symbol)
    }
    val bytes = classWriter.toByteArray()

    val define = DeleteClassLoader().define(name, bytes)

    val mirror = scope.universe.findInstanceKlass(name)!!
    mirror.constantPool.buildIndices()

    Objects.requireNonNull(define)

    return symbols.map { symbol ->
        val index = mirror.constantPool.getUtf8SymbolIndex(symbol)
        if (index == -1) {
            throw IllegalStateException("Failed to find symbol in constant pool")
        }
        val symbol1 = mirror.constantPool.getSymbol(index)
        //symbol1.increaseRefCount() // make sure gc cannot kill this symbol
        symbol1
    }
}

private val loaders = mutableListOf<DeleteClassLoader>()

fun defineClass(name: String, bytes: ByteArray): Class<*> {
    val loader = DeleteClassLoader()
    loaders.add(loader)
    return loader.define(name, bytes)
}

fun defineClass(bytes: ByteArray): Class<*> {
    val loader = DeleteClassLoader()
    loaders.add(loader)
    return loader.define(bytes)
}