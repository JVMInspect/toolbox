package land.src.toolbox.jvm.util

// Java Bytecodes
const val NOP = 0
const val ACONST_NULL = 1
const val ICONST_M1 = 2
const val ICONST_0 = 3
const val ICONST_1 = 4
const val ICONST_2 = 5
const val ICONST_3 = 6
const val ICONST_4 = 7
const val ICONST_5 = 8
const val LCONST_0 = 9
const val LCONST_1 = 10
const val FCONST_0 = 11
const val FCONST_1 = 12
const val FCONST_2 = 13
const val DCONST_0 = 14
const val DCONST_1 = 15
const val BIPUSH = 16
const val SIPUSH = 17
const val LDC = 18
const val LDC_W = 19
const val LDC2_W = 20
const val ILOAD = 21
const val LLOAD = 22
const val FLOAD = 23
const val DLOAD = 24
const val ALOAD = 25
const val ILOAD_0 = 26
const val ILOAD_1 = 27
const val ILOAD_2 = 28
const val ILOAD_3 = 29
const val LLOAD_0 = 30
const val LLOAD_1 = 31
const val LLOAD_2 = 32
const val LLOAD_3 = 33
const val FLOAD_0 = 34
const val FLOAD_1 = 35
const val FLOAD_2 = 36
const val FLOAD_3 = 37
const val DLOAD_0 = 38
const val DLOAD_1 = 39
const val DLOAD_2 = 40
const val DLOAD_3 = 41
const val ALOAD_0 = 42
const val ALOAD_1 = 43
const val ALOAD_2 = 44
const val ALOAD_3 = 45
const val IALOAD = 46
const val LALOAD = 47
const val FALOAD = 48
const val DALOAD = 49
const val AALOAD = 50
const val BALOAD = 51
const val CALOAD = 52
const val SALOAD = 53
const val ISTORE = 54
const val LSTORE = 55
const val FSTORE = 56
const val DSTORE = 57
const val ASTORE = 58
const val ISTORE_0 = 59
const val ISTORE_1 = 60
const val ISTORE_2 = 61
const val ISTORE_3 = 62
const val LSTORE_0 = 63
const val LSTORE_1 = 64
const val LSTORE_2 = 65
const val LSTORE_3 = 66
const val FSTORE_0 = 67
const val FSTORE_1 = 68
const val FSTORE_2 = 69
const val FSTORE_3 = 70
const val DSTORE_0 = 71
const val DSTORE_1 = 72
const val DSTORE_2 = 73
const val DSTORE_3 = 74
const val ASTORE_0 = 75
const val ASTORE_1 = 76
const val ASTORE_2 = 77
const val ASTORE_3 = 78
const val IASTORE = 79
const val LASTORE = 80
const val FASTORE = 81
const val DASTORE = 82
const val AASTORE = 83
const val BASTORE = 84
const val CASTORE = 85
const val SASTORE = 86
const val POP = 87
const val POP2 = 88
const val DUP = 89
const val DUP_X1 = 90
const val DUP_X2 = 91
const val DUP2 = 92
const val DUP2_X1 = 93
const val DUP2_X2 = 94
const val SWAP = 95
const val IADD = 96
const val LADD = 97
const val FADD = 98
const val DADD = 99
const val ISUB = 100
const val LSUB = 101
const val FSUB = 102
const val DSUB = 103
const val IMUL = 104
const val LMUL = 105
const val FMUL = 106
const val DMUL = 107
const val IDIV = 108
const val LDIV = 109
const val FDIV = 110
const val DDIV = 111
const val IREM = 112
const val LREM = 113
const val FREM = 114
const val DREM = 115
const val INEG = 116
const val LNEG = 117
const val FNEG = 118
const val DNEG = 119
const val ISHL = 120
const val LSHL = 121
const val ISHR = 122
const val LSHR = 123
const val IUSHR = 124
const val LUSHR = 125
const val IAND = 126
const val LAND = 127
const val IOR = 128
const val LOR = 129
const val IXOR = 130
const val LXOR = 131
const val IINC = 132
const val I2L = 133
const val I2F = 134
const val I2D = 135
const val L2I = 136
const val L2F = 137
const val L2D = 138
const val F2I = 139
const val F2L = 140
const val F2D = 141
const val D2I = 142
const val D2L = 143
const val D2F = 144
const val I2B = 145
const val I2C = 146
const val I2S = 147
const val LCMP = 148
const val FCMPL = 149
const val FCMPG = 150
const val DCMPL = 151
const val DCMPG = 152
const val IFEQ = 153
const val IFNE = 154
const val IFLT = 155
const val IFGE = 156
const val IFGT = 157
const val IFLE = 158
const val IF_ICMPEQ = 159
const val IF_ICMPNE = 160
const val IF_ICMPLT = 161
const val IF_ICMPGE = 162
const val IF_ICMPGT = 163
const val IF_ICMPLE = 164
const val IF_ACMPEQ = 165
const val IF_ACMPNE = 166
const val GOTO = 167
const val JSR = 168
const val RET = 169
const val TABLESWITCH = 170
const val LOOKUPSWITCH = 171
const val IRETURN = 172
const val LRETURN = 173
const val FRETURN = 174
const val DRETURN = 175
const val ARETURN = 176
const val RETURN = 177
const val GETSTATIC = 178
const val PUTSTATIC = 179
const val GETFIELD = 180
const val PUTFIELD = 181
const val INVOKEVIRTUAL = 182
const val INVOKESPECIAL = 183
const val INVOKESTATIC = 184
const val INVOKEINTERFACE = 185
const val INVOKEDYNAMIC = 186
const val NEW = 187
const val NEWARRAY = 188
const val ANEWARRAY = 189
const val ARRAYLENGTH = 190
const val ATHROW = 191
const val CHECKCAST = 192
const val INSTANCEOF = 193
const val MONITORENTER = 194
const val MONITOREXIT = 195
const val WIDE = 196
const val MULTIANEWARRAY = 197
const val IFNULL = 198
const val IFNONNULL = 199
const val GOTO_W = 200
const val JSR_W = 201
const val BREAKPOINT = 202

// JVM bytecodes
const val FAST_AGETFIELD = 203
const val FAST_BGETFIELD = 204
const val FAST_CGETFIELD = 205
const val FAST_DGETFIELD = 206
const val FAST_FGETFIELD = 207
const val FAST_IGETFIELD = 208
const val FAST_LGETFIELD = 209
const val FAST_SGETFIELD = 210
const val FAST_APUTFIELD = 211
const val FAST_BPUTFIELD = 212
const val FAST_ZPUTFIELD = 213
const val FAST_CPUTFIELD = 214
const val FAST_DPUTFIELD = 215
const val FAST_FPUTFIELD = 216
const val FAST_IPUTFIELD = 217
const val FAST_LPUTFIELD = 218
const val FAST_SPUTFIELD = 219
const val FAST_ALOAD_0 = 220
const val FAST_IACCESS_0 = 221
const val FAST_AACCESS_0 = 222
const val FAST_FACCESS_0 = 223
const val FAST_ILOAD = 224
const val FAST_ILOAD2 = 225
const val FAST_ICALOAD = 226
const val FAST_INVOKEVFINAL = 227
const val FAST_LINEARSWITCH = 228
const val FAST_BINARYSWITCH = 229
const val FAST_ALDC = 230
const val FAST_ALDC_W = 231
const val RETURN_REGISTER_FINALIZER = 232
const val INVOKEHANDLE = 233

// Bytecodes rewritten at CDS dump time
const val NOFAST_GETFIELD = 234
const val NOFAST_PUTFIELD = 235
const val NOFAST_ALOAD_0 = 236
const val NOFAST_ILOAD = 237
const val SHOULDNOTREACHHERE = 238 // For Debugging

val OPCODE_NAMES: Array<String> = Array(256) { "UNKNOWN" }

// fill in the opcode names
// get this class name

fun initOpcodeNames() {
    val clazz = Class.forName("land.src.toolbox.jvm.util.BytecodeConstantsKt")
    val fields = clazz.declaredFields
    for (field in fields) {
        if (field.type == Int::class.java) {
            val name = field.name
            val value = field.get(null) as Int
            OPCODE_NAMES[value] = name
        }
    }
}

fun opcodeName(opcode: Int): String {
    if (OPCODE_NAMES[0] == "UNKNOWN") {
        initOpcodeNames()
    }
    if (opcode < 0 || opcode >= OPCODE_NAMES.size) {
        return "UNKNOWN"
    }
    return OPCODE_NAMES[opcode]
}