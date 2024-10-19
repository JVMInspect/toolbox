package land.src.jvmtb.jvm.oop

import land.src.jvmtb.dsl.array
import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct
import land.src.jvmtb.jvm.VMVersion
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class InstanceKlass(address: Address) : Klass(address) {
    //inline fun <reified A : Any> api(vararg apis: Pair<String, KClass<A>>) =
    //    ReadOnlyProperty<Struct, A> { thisRef, property ->
    //        val major = structs<VMVersion>().major.toString()
    //        val apiClass = apis.find { it.first == major }!!.second
    //        structs(apiClass, address.base) as A
    //    }
//
    //abstract class Api(address: Address) : Struct(address) {
    //    abstract val majorVersion: Int
    //    abstract val minorVersion: Int
    //    abstract val accessFlags: AccessFlags
    //}
//
    //class Jdk8(address: Address) : Api(address) {
//
    //}
//
    //private val api by api("8" to Jdk8::class)

    val majorVersion: Int get() = TODO()
    val minorVersion: Int get() = TODO()
    val accessFlags: AccessFlags get() = TODO()

    val localInterfaces: Array<InstanceKlass>? get() = TODO()

    val methods: Array<Method> by array("_methods")
    val methodOrdering: Array<Int> by array("_method_ordering")
}