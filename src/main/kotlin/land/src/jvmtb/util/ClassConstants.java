package land.src.jvmtb.util;

public interface ClassConstants
{
    // constant pool constant types - from JVM spec.
    int JVM_CONSTANT_Utf8               = 1;
    int JVM_CONSTANT_Unicode            = 2; // unused
    int JVM_CONSTANT_Integer            = 3;
    int JVM_CONSTANT_Float              = 4;
    int JVM_CONSTANT_int               = 5;
    int JVM_CONSTANT_Double             = 6;
    int JVM_CONSTANT_Class              = 7;
    int JVM_CONSTANT_String             = 8;
    int JVM_CONSTANT_Fieldref           = 9;
    int JVM_CONSTANT_Methodref          = 10;
    int JVM_CONSTANT_InterfaceMethodref = 11;
    int JVM_CONSTANT_NameAndType        = 12;
    int JVM_CONSTANT_MethodHandle       = 15;
    int JVM_CONSTANT_MethodType         = 16;
    int JVM_CONSTANT_Dynamic            = 17;
    int JVM_CONSTANT_InvokeDynamic      = 18;
    int JVM_CONSTANT_Module             = 19;
    int JVM_CONSTANT_Package            = 20;

    // JVM_CONSTANT_MethodHandle subtypes
    int JVM_REF_getField                = 1;
    int JVM_REF_getStatic               = 2;
    int JVM_REF_putField                = 3;
    int JVM_REF_putStatic               = 4;
    int JVM_REF_invokeVirtual           = 5;
    int JVM_REF_invokeStatic            = 6;
    int JVM_REF_invokeSpecial           = 7;
    int JVM_REF_newInvokeSpecial        = 8;
    int JVM_REF_invokeInterface         = 9;

    // HotSpot specific constant pool constant types.

    // For bad value initialization

    int JVM_CONSTANT_Invalid            = 0;
    int JVM_CONSTANT_UnresolvedClass          = 100;  // Temporary tag until actual use
    int JVM_CONSTANT_ClassIndex               = 101;  // Temporary tag while constructing constant pool
    int JVM_CONSTANT_StringIndex              = 102;  // Temporary tag while constructing constant pool
    int JVM_CONSTANT_UnresolvedClassInError   = 103;  // Error tag due to resolution error
    int JVM_CONSTANT_MethodHandleInError      = 104;  // Error tag due to resolution error
    int JVM_CONSTANT_MethodTypeInError        = 105;  // Error tag due to resolution error

    // 1.5 major/minor version numbers from JVM spec. 3rd edition

    short MAJOR_VERSION = 49;
    short MINOR_VERSION = 0;
    short MAJOR_VERSION_OLD = 46;
    short MINOR_VERSION_OLD = 0;

    // From jvm.h
    int JVM_ACC_PUBLIC       = 0x0001; /* visible to everyone */
    int JVM_ACC_PRIVATE      = 0x0002; /* visible only to the defining class */
    int JVM_ACC_PROTECTED    = 0x0004; /* visible to subclasses */
    int JVM_ACC_STATIC       = 0x0008; /* instance variable is static */
    int JVM_ACC_FINAL        = 0x0010; /* no further subclassing; overriding */
    int JVM_ACC_SYNCHRONIZED = 0x0020; /* wrap method call in monitor lock */
    int JVM_ACC_SUPER        = 0x0020; /* funky handling of invokespecial */
    int JVM_ACC_VOLATILE     = 0x0040; /* can not cache in registers */
    int JVM_ACC_BRIDGE       = 0x0040; /* bridge method generated by compiler */
    int JVM_ACC_TRANSIENT    = 0x0080; /* not persistent */
    int JVM_ACC_VARARGS      = 0x0080; /* method declared with variable number of args */
    int JVM_ACC_NATIVE       = 0x0100; /* implemented in C */
    int JVM_ACC_INTERFACE    = 0x0200; /* class is an interface */
    int JVM_ACC_ABSTRACT     = 0x0400; /* no definition provided */
    int JVM_ACC_STRICT       = 0x0800; /* strict floating point */
    int JVM_ACC_SYNTHETIC    = 0x1000; /* compiler-generated class; method or field */
    int JVM_ACC_ANNOTATION   = 0x2000; /* annotation type */
    int JVM_ACC_ENUM         = 0x4000; /* field is declared as element of enum */

    // from accessFlags.hpp - hotspot internal flags

    // flags actually put in .class file
    int JVM_ACC_WRITTEN_FLAGS = 0x00007FFF;

    // flags accepted by set_field_flags
    int JVM_ACC_FIELD_FLAGS = 0x00008000 | JVM_ACC_WRITTEN_FLAGS;

    // from jvm.h
    int JVM_RECOGNIZED_CLASS_MODIFIERS   = (JVM_ACC_PUBLIC |
                                                                 JVM_ACC_FINAL |
                                                                 JVM_ACC_SUPER |
                                                                 JVM_ACC_INTERFACE |
                                                                 JVM_ACC_ABSTRACT |
                                                                 JVM_ACC_ANNOTATION |
                                                                 JVM_ACC_ENUM |
                                                                 JVM_ACC_SYNTHETIC);

    int JVM_RECOGNIZED_FIELD_MODIFIERS  = (JVM_ACC_PUBLIC |
                                                                JVM_ACC_PRIVATE |
                                                                JVM_ACC_PROTECTED |
                                                                JVM_ACC_STATIC |
                                                                JVM_ACC_FINAL |
                                                                JVM_ACC_VOLATILE |
                                                                JVM_ACC_TRANSIENT |
                                                                JVM_ACC_ENUM |
                                                                JVM_ACC_SYNTHETIC);

    int JVM_RECOGNIZED_METHOD_MODIFIERS  = (JVM_ACC_PUBLIC |
                                                                 JVM_ACC_PRIVATE |
                                                                 JVM_ACC_PROTECTED |
                                                                 JVM_ACC_STATIC |
                                                                 JVM_ACC_FINAL |
                                                                 JVM_ACC_SYNCHRONIZED |
                                                                 JVM_ACC_BRIDGE |
                                                                 JVM_ACC_VARARGS |
                                                                 JVM_ACC_NATIVE |
                                                                 JVM_ACC_ABSTRACT |
                                                                 JVM_ACC_STRICT |
                                                                 JVM_ACC_SYNTHETIC);

    // hotspot specific internal flags

    // Method* flags
    int JVM_ACC_MONITOR_MATCH           = 0x10000000;     // True if we know that monitorenter/monitorexit bytecodes match
    int JVM_ACC_HAS_MONITOR_BYTECODES   = 0x20000000;     // Method contains monitorenter/monitorexit bytecodes
    int JVM_ACC_HAS_LOOPS               = 0x40000000;     // Method has loops
    int JVM_ACC_LOOPS_FLAG_INIT         = (int)0x80000000;// The loop flag has been initialized
    int JVM_ACC_QUEUED                  = 0x01000000;     // Queued for compilation
    int JVM_ACC_NOT_C2_COMPILABLE       = 0x02000000;
    int JVM_ACC_NOT_C1_COMPILABLE       = 0x04000000;
    int JVM_ACC_NOT_C2_OSR_COMPILABLE   = 0x08000000;
    int JVM_ACC_HAS_LINE_NUMBER_TABLE   = 0x00100000;
    int JVM_ACC_HAS_CHECKED_EXCEPTIONS  = 0x00400000;
    int JVM_ACC_HAS_JSRS                = 0x00800000;
    int JVM_ACC_IS_OLD                  = 0x00010000;     // RedefineClasses() has replaced this method
    int JVM_ACC_IS_OBSOLETE             = 0x00020000;     // RedefineClasses() has made method obsolete
    int JVM_ACC_IS_PREFIXED_NATIVE      = 0x00040000;     // JVMTI has prefixed this native method
    int JVM_ACC_ON_STACK                = 0x00080000;     // RedefineClasses() was used on the stack
    int JVM_ACC_IS_DELETED              = 0x00008000;     // RedefineClasses() has deleted this method

    // Klass* flags
    int JVM_ACC_HAS_MIRANDA_METHODS     = 0x10000000;     // True if this class has miranda methods in it's vtable
    int JVM_ACC_HAS_VANILLA_CONSTRUCTOR = 0x20000000;     // True if klass has a vanilla default constructor
    int JVM_ACC_HAS_FINALIZER           = 0x40000000;     // True if klass has a non-empty finalize() method
    int JVM_ACC_IS_CLONEABLE_FAST       = (int)0x80000000;// True if klass implements the Cloneable interface and can be optimized in generated code
    int JVM_ACC_HAS_FINAL_METHOD        = 0x01000000;     // True if klass has final method
    int JVM_ACC_IS_SHARED_CLASS         = 0x02000000;     // True if klass is shared
    int JVM_ACC_IS_HIDDEN_CLASS         = 0x04000000;     // True if klass is hidden
    int JVM_ACC_IS_VALUE_BASED_CLASS    = 0x08000000;     // True if klass is marked as a ValueBased class

    // Klass* and Method* flags
    int JVM_ACC_HAS_LOCAL_VARIABLE_TABLE= 0x00200000;

    int JVM_ACC_PROMOTED_FLAGS          = 0x00200000;     // flags promoted from methods to the holding klass

    // field flags
    // Note: these flags must be defined in the low order 16 bits because
    // InstanceKlass only stores a ushort worth of information from the
    // AccessFlags value.
    // These bits must not conflict with any other field-related access flags
    // (e.g.; ACC_ENUM).
    // Note that the class-related ACC_ANNOTATION bit conflicts with these flags.
    int JVM_ACC_FIELD_ACCESS_WATCHED            = 0x00002000; // field access is watched by JVMTI
    int JVM_ACC_FIELD_MODIFICATION_WATCHED      = 0x00008000; // field modification is watched by JVMTI
    int JVM_ACC_FIELD_INTERNAL                  = 0x00000400; // internal field; same as JVM_ACC_ABSTRACT
    int JVM_ACC_FIELD_STABLE                    = 0x00000020; // @Stable field; same as JVM_ACC_SYNCHRONIZED and JVM_ACC_SUPER
    int JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE  = 0x00000100; // (static) final field updated outside (class) initializer; same as JVM_ACC_NATIVE
    int JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE     = 0x00000800; // field has generic signature

}