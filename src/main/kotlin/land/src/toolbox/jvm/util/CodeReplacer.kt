package land.src.toolbox.jvm.util

import dev.xdark.blw.classfile.ClassFileView
import land.src.toolbox.jvm.VirtualMachine
import land.src.toolbox.jvm.oop.InstanceKlass
import land.src.toolbox.jvm.oop.Method

class CodeReplacer(val vm: VirtualMachine, val file: ClassFileView, val target: Method) {

    fun replace() {
        // load the bytecode from the file and replace the target method with it

        // first step is we need to patch the constant pool to make sure all the references are correct
        // since looking up and double-checking overlaps is a pain, we'll just add on

        val pool = target.constMethod.constants

        val expandInformation = pool.expand(file.constantPool()!!.stream().toList())

        // now we patch the method itself
        TODO("go over bytecode, for member references lookup cache index and place that, the rest requires no alteration")
        TODO("do NOT use fast_aldc or any rewrite instructions, ultimate complexity")
        TODO("invokedynamic, just dont use it, its a pain to lookup references")

    }

}