package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.oop.ConstMethod
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
        FAST_ILOAD2 to -1,
        FAST_ICALOAD to -1,
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
        NOFAST_ILOAD to ILOAD,
        INVALID to NOP
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
            in GETSTATIC..INVOKEINTERFACE,
            IFNULL, IFNONNULL,
            SIPUSH, IINC, LDC_W, LDC2_W,
            ANEWARRAY, CHECKCAST, INSTANCEOF, NEW, GOTO, JSR -> 2

            MULTIANEWARRAY -> 3

            GOTO_W, JSR_W, INVOKEDYNAMIC -> 4

            else -> 0
        }
    }

    fun isMemberAccess(jvm: Int) =
        jvm in GETSTATIC..INVOKEINTERFACE

    private fun readShort(code: ByteArray, index: Int, bigEndian: Boolean): Short {
        val high = code[index].toInt() and 0xff
        val low = code[index + 1].toInt() and 0xff
        return if (bigEndian) ((high shl 8) or low).toShort()
        else ((low shl 8) or high).toShort()
    }

    private fun readInt(code: ByteArray, index: Int, bigEndian: Boolean): Int {
        val highShort = readShort(code, index, bigEndian).toInt() and 0xffff
        val lowShort = readShort(code, index + 2, bigEndian).toInt() and 0xffff
        return if (bigEndian) (highShort shl 16) or lowShort
        else (lowShort shl 16) or highShort
    }

    private fun writeShort(code: ByteArray, index: Int, value: Short, bigEndian: Boolean) {
        val high = (value.toInt() shr 8).toByte()
        val low = value.toByte()
        if (bigEndian) {
            code[index] = high
            code[index + 1] = low
        } else {
            code[index] = low
            code[index + 1] = high
        }
    }

    val rewrittenCode: ByteArray by lazy rewrittenCode@{
        var bci = 0
        val code = method.code
        val rewritten = code.copyOf(code.size)
        val pool = method.constants

        if (pool.cache == null) {
            return@rewrittenCode rewritten
        }

        while (bci < code.size) {
            val jvm = code[bci].toInt() and 0xff
            val java = java(jvm)
            rewritten[bci] = java.toByte()

            when {
                jvm == FAST_ILOAD2 -> {
                    rewritten[bci] = ILOAD.toByte()
                    rewritten[bci + 2] = ILOAD.toByte()
                    bci += 3
                }
                jvm == FAST_ICALOAD -> {
                    rewritten[bci] = IALOAD.toByte()
                    rewritten[bci + 2] = CALOAD.toByte()
                    bci += 3
                }
                jvm in FAST_AGETFIELD..FAST_SPUTFIELD -> {
                    val index = readShort(code, bci + 1, false)
                    val refIndex = pool.getRefIndex(index)
                    writeShort(rewritten, bci + 1, refIndex, true)
                }
                jvm == FAST_IACCESS_0 || jvm == FAST_AACCESS_0 -> {
                    rewritten[bci + 1] = GETFIELD.toByte()
                    val index = readShort(code, bci + 2, false)
                    val refIndex = pool.getRefIndex(index)
                    writeShort(rewritten, bci + 2, refIndex, true)
                }
                jvm == FAST_ALDC -> {
                    val index = (code[bci + 1].toInt() and 0xff).toShort()
                    rewritten[bci + 1] = pool.getObjectIndex(index).toByte()
                }
                jvm == FAST_ALDC_W -> {
                    val index = readShort(code, bci + 1, false)
                    val refIndex = pool.getObjectIndex(index)
                    writeShort(rewritten, bci + 1, refIndex, true)
                }
                jvm == INVOKEHANDLE -> {
                    val index = readShort(code, bci + 1, false)
                    val refIndex = pool.getRefIndex(index)
                    writeShort(rewritten, bci + 1, refIndex, true)
                }
                isMemberAccess(java) -> {
                    val index = readShort(code, bci + 1, false)
                    val refIndex = pool.getRefIndex(index)
                    writeShort(rewritten, bci + 1, refIndex, true)
                }
                java == LOOKUPSWITCH -> {
                    val alignedBci = roundTo(bci + 1, 4)
                    val npairs = readInt(code, alignedBci + 4, true)
                    val len = (alignedBci - bci) + (2 + 2*npairs)*4

                    bci += len

                    continue
                }
                java == TABLESWITCH -> {
                    val alignedBci = roundTo(bci + 1, 4)
                    val low = readInt(code, alignedBci + 4, true)
                    val high = readInt(code, alignedBci + 8, true)
                    val len = (alignedBci - bci) + (3 + high - low + 1)*4

                    bci += len

                    continue
                }
                java == INVOKEDYNAMIC -> {
                    val index = readShort(code, bci + 1, false).inv()
                    val actual = pool.cache!![index.toInt()].cpIndex
                    writeShort(rewritten, bci + 1, actual.toShort(), true)
                    writeShort(rewritten, bci + 3, 0, true)
                }
                java == WIDE -> {
                    val opcode = code[bci + 1].toInt() and 0xff
                    bci += 1 // skip opcode
                    bci += when (opcode) {
                        IINC -> 4
                        else -> 2
                    }
                }
            }

            bci += 1 + operands(java)
        }

        return@rewrittenCode rewritten
    }
}