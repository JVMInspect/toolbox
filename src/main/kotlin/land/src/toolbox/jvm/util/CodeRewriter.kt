package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.oop.ConstMethod
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.inv

class CodeRewriter(val method: ConstMethod) {
    private val jvmToJavaCodes = mapOf(
        FAST_AGETFIELD to GETFIELD,
        FAST_BGETFIELD to GETFIELD,
        FAST_CGETFIELD to GETFIELD,
        FAST_DGETFIELD to GETFIELD,
        FAST_FGETFIELD to GETFIELD,
        FAST_IGETFIELD to GETFIELD,
        FAST_LGETFIELD to GETFIELD,
        FAST_SGETFIELD to GETFIELD,
        FAST_APUTFIELD to PUTFIELD,
        FAST_BPUTFIELD to PUTFIELD,
        FAST_ZPUTFIELD to PUTFIELD,
        FAST_CPUTFIELD to PUTFIELD,
        FAST_DPUTFIELD to PUTFIELD,
        FAST_FPUTFIELD to PUTFIELD,
        FAST_IPUTFIELD to PUTFIELD,
        FAST_LPUTFIELD to PUTFIELD,
        FAST_SPUTFIELD to PUTFIELD,
        FAST_ALOAD_0 to ALOAD_0,
        FAST_IACCESS_0 to ALOAD_0,
        FAST_AACCESS_0 to ALOAD_0,
        FAST_FACCESS_0 to ALOAD_0,
        FAST_ILOAD to ILOAD,
        FAST_ILOAD2 to ILOAD,
        FAST_ICALOAD to ILOAD,
        FAST_INVOKEVFINAL to INVOKEVIRTUAL,
        FAST_LINEARSWITCH to LOOKUPSWITCH,
        FAST_BINARYSWITCH to LOOKUPSWITCH,
        FAST_ALDC to LDC,
        FAST_ALDC_W to LDC_W,
        RETURN_REGISTER_FINALIZER to RETURN,
        INVOKEHANDLE to INVOKEVIRTUAL,
        NOFAST_GETFIELD to GETFIELD,
        NOFAST_PUTFIELD to PUTFIELD,
        NOFAST_ALOAD_0 to ALOAD_0,
        NOFAST_ILOAD to ILOAD
    )

    fun java(code: Int) =
        jvmToJavaCodes[code] ?: code

    fun operands(code: Int): Int {
        return when (code) {
            in NOP..DCONST_1,
            in ILOAD_0..SALOAD,
            in ISTORE_0..LXOR,
            in I2L..DCMPG,
            in IRETURN..RETURN,
            ARRAYLENGTH, ATHROW, MONITORENTER, MONITOREXIT, BREAKPOINT -> 0

            in ILOAD..ALOAD,
            in ISTORE..ASTORE,
            RET, BIPUSH, NEWARRAY, LDC -> 1

            in IFEQ..IF_ACMPNE,
            IFNULL, IFNONNULL,
            SIPUSH, IINC, LDC_W, LDC2_W,
            ANEWARRAY, CHECKCAST, INSTANCEOF, NEW, GOTO, JSR -> 2

            MULTIANEWARRAY -> 3

            GOTO_W, JSR_W -> 4

            else -> 0
        }
    }

    fun isMemberAccess(jvm: Int) =
        jvm in GETSTATIC..INVOKEINTERFACE

    fun readShort(code: ByteArray, index: Int, bigEndian: Boolean): Short {
        return if (bigEndian) {
            (((code[index].toInt() and 0xff) shl 8) or (code[index + 1].toInt() and 0xff)).toShort()
        } else {
            (((code[index + 1].toInt() and 0xff) shl 8) or (code[index].toInt() and 0xff)).toShort()
        }
    }

    fun readInt(code: ByteArray, index: Int, bigEndian: Boolean): Int {
        if (bigEndian) {
            return readShort(code, index, true).toInt() shl 16 or readShort(code, index + 2, true).toInt()
        }
        return readShort(code, index + 2, false).toInt() shl 16 or readShort(code, index, false).toInt()
    }

    fun writeShort(code: ByteArray, index: Int, value: Short, bigEndian: Boolean) {
        if (bigEndian) {
            code[index] = (value.toInt() shr 8).toByte()
            code[index + 1] = value.toByte()
        } else {
            code[index] = value.toByte()
            code[index + 1] = (value.toInt() shr 8).toByte()
        }
    }

    val rewrittenCode: ByteArray by lazy rewrittenCode@{

        var bci = 0
        val code = method.code
        val rewritten = ByteArray(code.size)
        val constantPool = method.constants
        val cache = constantPool.cache

        println("rewriting ${method.constants.getString(method.nameIndex.toInt())}")

        while (bci < code.size) {
            val jvm = code[bci].toInt() and 0xff
            val java = java(jvm)
            val operands = operands(jvm)
            rewritten[bci] = (java and 0xff).toByte()

            //if (0 > operands) {
            //    error("operands: $operands")
            //}

            //bci++

            //println("bci: $bci, java: $java, jvm: $jvm")

            when {
                jvm == FAST_ILOAD2 -> {
                    rewritten[bci + 2] = ILOAD.toByte()
                    bci += 3
                    println("handled FAST_ILOAD2 (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                }
                jvm == FAST_ICALOAD -> {
                    rewritten[bci + 2] = CALOAD.toByte()
                    bci += 3

                    println("handled FAST_ICALOAD (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                }
                jvm in FAST_AGETFIELD..FAST_SPUTFIELD -> {
                    val index = readShort(code, bci + 1, false)
                    val refIndex = constantPool.getRefIndex(index)
                    writeShort(rewritten, bci + 1, refIndex, true)
                    bci += 2
                    println("handled FAST_AGETFIELD..FAST_SPUTFIELD (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                }
                jvm == FAST_IACCESS_0 || jvm == FAST_AACCESS_0 -> {
                    rewritten[bci + 1] = GETFIELD.toByte()
                    val index = readShort(code, bci + 2, false)
                    val refIndex = constantPool.getRefIndex(index)
                    writeShort(rewritten, bci + 2, refIndex, true)
                    bci += 3
                    println("handled FAST_IACCESS_0..FAST_AACCESS_0 (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                }
                jvm == FAST_ALDC -> {
                    val index = (code[bci + 1].toInt() and 0xff).toShort()
                    rewritten[bci + 1] = cache.referenceMap[index.toInt()]?.toByte()!!
                    bci++
                    println("handled FAST_ALDC (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                }
                jvm == FAST_ALDC_W -> {
                    val index = readShort(code, bci + 1, false)
                    val refIndex = cache.referenceMap[index.toInt()]!!
                    writeShort(rewritten, bci + 1, refIndex, true)
                    bci += 2
                    println("handled FAST_ALDC_W (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                }
                jvm == INVOKEHANDLE -> {
                    val index = readShort(code, bci + 1, false)
                    val refIndex = constantPool.getRefIndex(index)
                    writeShort(rewritten, bci + 1, refIndex, true)
                    println("handled INVOKEHANDLE (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                    bci += 2
                }
                isMemberAccess(java) -> {
                    val index = readShort(code, bci + 1, false)
                    val refIndex = constantPool.cache[index.toInt()].cpIndex.toShort()
                    writeShort(rewritten, bci + 1, refIndex, true)
                    println("handled isMemberAccess (bci: ${bci - 1}, jvm: $jvm, java: $java)")

                    bci += 2
                }
                java == LOOKUPSWITCH -> {
                    val originalBci = bci - 1
                    bci += 4 - (bci and 3) + 4
                    val pairs = readShort(code, bci, true)
                    bci += 4 + pairs * 8
                    if (jvm == FAST_LINEARSWITCH || jvm == FAST_BINARYSWITCH) {
                        bci--
                    }
                    println("handled LOOKUPSWITCH (bci: ${originalBci}, jvm: $jvm, java: $java)")
                }
                java == TABLESWITCH -> {
                    bci += 4 - (bci and 3) + 4
                    val low = readInt(code, bci, true)
                    bci += 4
                    val high = readInt(code, bci, true)
                    bci += 4
                    val count = high - low + 1
                    bci += count * 4
                    bci -= 1

                    println("handled TABLESWITCH (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                }
                java == INVOKEDYNAMIC -> {
                    val index = readShort(code, bci + 1, false).inv()
                    val actual = constantPool.cache[index.toInt()].cpIndex
                    writeShort(rewritten, bci + 1, actual.toShort(), true)
                    writeShort(rewritten, bci + 3, 0, true)
                    bci += 4

                    println("handled INVOKEDYNAMIC")
                }
                java == WIDE -> {
                    val opcode = code[bci + 1].toInt() and 0xff
                    bci += 1 // skip opcode
                    bci += when (opcode) {
                        IINC -> 4
                        else -> 2
                    }

                    println("handled WIDE (bci: ${bci - 1}, jvm: $jvm, java: $java)")
                }
                else -> {
                    println("handled opcode: $java, $jvm, operands: $operands, bci: $bci")
                    bci += operands
                }
            }

            bci++
        }

        return@rewrittenCode rewritten
    }
}