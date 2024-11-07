package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.VirtualMachine
import land.src.toolbox.jvm.oop.Method
import software.coley.cafedude.classfile.attribute.CodeAttribute
import software.coley.cafedude.classfile.constant.ConstRef
import software.coley.cafedude.classfile.constant.CpClass
import software.coley.cafedude.classfile.constant.CpDynamic
import software.coley.cafedude.classfile.constant.CpInvokeDynamic
import software.coley.cafedude.classfile.constant.CpNameType
import software.coley.cafedude.classfile.constant.CpString
import software.coley.cafedude.classfile.instruction.CpRefInstruction
import software.coley.cafedude.io.ClassFileReader
import software.coley.cafedude.io.InstructionWriter

class CodeReplacer(val vm: VirtualMachine, val target: Method) {

    fun replace(classBytes: ByteArray) {
        // load the bytecode from the file and replace the target method with it
        val classFileReader = ClassFileReader()

        val classFile = classFileReader.read(classBytes)

        // first step is we need to patch the constant pool to make sure all the references are correct
        // since looking up and double-checking overlaps is a pain, we'll just add on

        val pool = target.constMethod.constants
        val method = classFile.methods.find { it.name.text == target.name && it.type.text == target.signature } ?: return

        // get the cp data only used by that method and expand the pool

        val filtered = method.cpAccesses()

        // hack: cafedude doesnt add sub entries to the pool, so we need to do it manually
        method.cpAccesses().forEach {
            when (it) {
                is CpClass -> filtered.add(it.name)
                is CpString -> filtered.add(it.string)
                is CpNameType -> {
                    filtered.add(it.name)
                    filtered.add(it.type)
                }
                is ConstRef -> {
                    filtered.add(it.classRef)
                    filtered.add(it.classRef.name)
                    filtered.add(it.nameType)
                    filtered.add(it.nameType.name)
                    filtered.add(it.nameType.type)
                }
                is CpInvokeDynamic -> {
                    filtered.add(it.nameType)
                    filtered.add(it.nameType.name)
                    filtered.add(it.nameType.type)
                }
                is CpDynamic -> {
                    filtered.add(it.nameType)
                    filtered.add(it.nameType.name)
                    filtered.add(it.nameType.type)
                }
            }
        }

        pool.buildIndices()

        val expandInformation = pool.expand(filtered.toMutableList().sortedBy { it.index }.toMutableList())

        // process bytecode
        val code = method.getAttribute(CodeAttribute::class.java)!!

        //TODO: update exception table information

        code.instructions.forEach {
            when (it.opcode) {
                in GETSTATIC..INVOKEINTERFACE -> {
                    val entry = (it as CpRefInstruction).entry

                    // hack the little endian version of the cache index into the `index` field
                    val cacheIndex = expandInformation.cacheMapping[entry.index] ?: return@forEach

                    // write cacheIndex in such a way that in big endian it represents the little endian value
                    val cacheIndexRev = java.lang.Short.reverseBytes(cacheIndex.toShort())

                    entry.index = cacheIndexRev.toInt()
                }
            }
        }

        val bytecode = InstructionWriter().writeCode(code.instructions)

        // TODO: resize the code array if needed
        val constMethod = target.constMethod
        for ((index, byte) in bytecode.withIndex()) {
            constMethod.code[index] = byte
        }

        target.constMethod.constants = expandInformation.pool

        target.constMethod.constants.poolHolder.constantPool = expandInformation.pool
    }

}