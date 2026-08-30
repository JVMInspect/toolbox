package land.src.toolbox.jvm

import land.src.toolbox.jvm.oop.ArrayOopDesc
import land.src.toolbox.jvm.oop.Klass
import land.src.toolbox.jvm.oop.OopDesc
import land.src.toolbox.jvm.oop.elemBytes
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.util.*

class Objects(val scope: Scope) {

    fun arrayKlass(type: Int): Klass {
        return when (type) {
            T_BYTE -> scope.vmClasses.bytePrimitiveArray
            T_CHAR -> scope.vmClasses.charPrimitiveArray
            T_SHORT -> scope.vmClasses.shortPrimitiveArray
            T_INT -> scope.vmClasses.intPrimitiveArray
            T_LONG -> scope.vmClasses.longPrimitiveArray
            T_FLOAT -> scope.vmClasses.floatPrimitiveArray
            T_DOUBLE -> scope.vmClasses.doublePrimitiveArray
            T_BOOLEAN -> scope.vmClasses.booleanPrimitiveArray
            T_OBJECT -> scope.vmClasses.objectArray
            T_NARROWOOP -> scope.vmClasses.objectArray
            T_NARROWKLASS -> scope.vmClasses.classArray
            else -> throw IllegalArgumentException("Unknown array type $type")
        }
    }

    fun initializeObject(oop: OopDesc, klass: Klass) {
        oop.klass = klass
        oop.markWord = 0 shr 1 or 1 // no_monitor_hash | no_bias_lock
    }

    private val localObjectsStorage = mutableMapOf<Long, Object>()
    private var byteArrayHeaderSize = 0

    fun allocateObjectMemory(size: Int): Long {
        if (scope.vm.isLocal) {
            if (byteArrayHeaderSize == 0) {
                byteArrayHeaderSize = ArrayOopDesc(scope.placeholder).headerSize(T_BYTE)
            }
            // we just create a local object
            val toAllocate = size - byteArrayHeaderSize
            val local = ByteArray(toAllocate) { 0 }

            val localObject = toVMObject(scope, local as Object)

            localObjectsStorage[localObject.address.base] = local as Object

            return localObject.address.base
        }
        // access the current heap
        val heap = scope.universe.collectedHeap!!
        val heapWord = heap.allocate(size.toLong())

        return heapWord.toLong()
    }

    fun allocateArray(type: Int, length: Int): ArrayOopDesc {
        val elemSize = elemBytes(type)
        val headerSize = ArrayOopDesc(scope.placeholder).headerSize(type)

        val size = headerSize + elemSize * length

        val address = allocateObjectMemory(size)

        println(java.lang.Long.toHexString(address))

        val array = ArrayOopDesc(Address(scope, address))

        initializeObject(array, arrayKlass(type))

        array.length = length

        return array
    }

}