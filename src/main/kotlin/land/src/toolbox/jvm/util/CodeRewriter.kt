package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.oop.ConstMethod
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

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
            RET, ARRAYLENGTH, ATHROW, MONITORENTER, MONITOREXIT, BREAKPOINT -> 0

            in ILOAD..ALOAD,
            in ISTORE..ASTORE,
            BIPUSH, NEWARRAY, LDC -> 1

            in IFEQ..IF_ACMPNE,
            IFNULL, IFNONNULL,
            SIPUSH, IINC, LDC_W, LDC2_W,
            ANEWARRAY, CHECKCAST, INSTANCEOF, NEW, GOTO, JSR -> 2

            MULTIANEWARRAY -> 3

            GOTO_W, JSR_W -> 4

            else -> -1
        }
    }

    fun isMemberAccess(jvm: Int) =
        jvm in FAST_AGETFIELD..FAST_SPUTFIELD

    val rewrittenCode: ByteArray by lazy rewrittenCode@{
        var bci = 0
        val bytes = method.code
        val code = ByteArray(bytes.size)
        while (bci < bytes.size) {
            val jvm = bytes[bci++].toInt()
            val java = java(jvm)
            val operands = operands(jvm)
            bytes[bci] = java.toByte()

            when {
                jvm == FAST_ILOAD2 -> {
                    bytes[bci + 2] = ILOAD.toByte()
                    bci += 3
                }
                jvm == FAST_ICALOAD -> {
                    bytes[bci + 2] = CALOAD.toByte()
                    bci += 3
                }
                isMemberAccess(java) -> {

                }
                java == LOOKUPSWITCH -> {

                }
                java == TABLESWITCH -> {

                }
                java == INVOKEDYNAMIC -> {

                }
                else -> bci += operands
            }
        }

        return@rewrittenCode code
    }
}