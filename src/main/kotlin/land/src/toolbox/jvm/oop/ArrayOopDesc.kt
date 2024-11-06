package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.primitive.Address

class ArrayOopDesc(address: Address) : OopDesc(address) {
    override val typeName: String = "arrayOopDesc"

    val length: Int get() {
        // when compressed class pointers is used the length is stored in the top half of the _metadata._narrow_klass field
        return if (useCompressedKlassPointers) {
            _klass.base.ushr(32).toInt()
        } else {
            // otherwise the length is stored right after the header
            getField<Int>(structs.sizeof(ArrayOopDesc::class))
        }
    }

    inline operator fun <reified T: Any> get(index: Int): T {
        return getField<T>(structs.sizeof(ArrayOopDesc::class) + index * 8)
    }

    inline operator fun <reified T: Any> set(index: Int, value: T) {
        setField(structs.sizeof(ArrayOopDesc::class) + index * 8, value)
    }
}