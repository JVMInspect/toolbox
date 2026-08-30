package land.src.toolbox.jvm.util

import java.lang.invoke.ConstantCallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.MutableCallSite

private val handle = MethodHandles.lookup().findStatic(String.javaClass, "valueOf", MethodType.methodType(String::class.java, CharArray::class.java))
private val site = MutableCallSite(MethodType.methodType(String::class.java, CharArray::class.java))

fun triggerDeoptimizationOfAllMarked() {

    // MHN.setTargetNormal, deoptimizes all depends of that method, but the call leads to
    // Deoptimization::deoptimize_all_marked(), which deoptimizes all marked nmethods regardless of the
    // target method.

    // updating the target of a MutableCallSite triggers deoptimization of all marked nmethods
    site.target = handle
}