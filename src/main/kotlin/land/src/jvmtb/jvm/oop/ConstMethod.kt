package land.src.jvmtb.jvm.oop

import land.src.jvmtb.jvm.Address
import land.src.jvmtb.jvm.Struct
import kotlin.Array

class ConstMethod {
   //class MethodFlags(address: Address) : Struct(address) {
   //    var HAS_LINENUMBER_TABLE: Int = 0
   //    var HAS_CHECKED_EXCEPTIONS: Int = 0
   //    var HAS_LOCALVARIABLE_TABLE: Int = 0
   //    var HAS_EXCEPTION_TABLE: Int = 0
   //    var HAS_GENERIC_SIGNATURE: Int = 0
   //    var HAS_METHOD_ANNOTATIONS: Int = 0
   //    var HAS_PARAMETER_ANNOTATIONS: Int = 0
   //    var HAS_METHOD_PARAMETERS: Int = 0
   //    var HAS_DEFAULT_ANNOTATIONS: Int = 0
   //    var HAS_TYPE_ANNOTATIONS: Int = 0
   //}

   //val methodFlags = MethodFlags()

    val genericSignatureIndex: Int get() = TODO()
    val hasStackMapTable: Boolean get() = TODO()
    val stackMapData: Array<Byte> get() = TODO()
    val hasLineNumberTable: Boolean get() = TODO()
    val lineNumberTableEntries: Int get() = TODO()
    val hasLocalVariableTable: Boolean get() = TODO()
    val nameIndex: Int get() = TODO()
    val flags: Int get() = TODO()
    val signatureIndex: Int get() = TODO()
    val codeSize: Int get() = TODO()
    val hasCheckedExceptions: Boolean get() = TODO()
    val hasMethodParameters: Boolean get() = TODO()


    /**
     * u2* ConstMethod::localvariable_table_length_addr() const {
     *   assert(has_localvariable_table(), "called only if table is present");
     *   if (has_exception_table()) {
     *     // If exception_table present, locate immediately before them.
     *     return (u2*) exception_table_start() - 1;
     *   } else {
     *     if (has_checked_exceptions()) {
     *       // If checked_exception present, locate immediately before them.
     *       return (u2*) checked_exceptions_start() - 1;
     *     } else {
     *       if(has_method_parameters()) {
     *         // If method parameters present, locate immediately before them.
     *         return (u2*)method_parameters_start() - 1;
     *       } else {
     *         // Else, the exception table is at the end of the constMethod.
     *       return has_generic_signature() ? (last_u2_element() - 1) :
     *                                         last_u2_element();
     *       }
     *     }
     *   }
     * }
     */
    val localVariableTableStart: Long get() = TODO()
    val localVariableTableLength: Int get() = TODO()

}