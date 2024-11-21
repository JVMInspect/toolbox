package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.dsl.nonNull
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

open class Thread(address: Address) : Struct(address) {

    val tlab: ThreadLocalAllocator by nonNull("_tlab", isPointer = false)

    companion object {
        fun current(scope: Scope): JavaThread {
            // address is the eeTop
            val thread = java.lang.Thread.currentThread()
            val eeTop = thread.javaClass.getDeclaredField("eetop").apply { isAccessible = true }.getLong(thread)

            return JavaThread(Address(scope, eeTop))
        }
    }

}