package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.Scope

enum class InstanceKlassFlag(private vararg val bits: Int) {
    REWRITTEN(0, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0),
    HAS_NONSTATIC_FIELDS(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    SHOULD_VERIFY_CLASS(2, 4, 4, 4, 4, 4, 4, 2, 2, 2, 2, 2, 2),
    IS_ANONYMOUS(3, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1),
    IS_CONTENDED(4, 5, 5, 5, 5, 5, 5, 3, 3, 3, 3),
    HAS_DEFAULT_METHODS,
    DECLARES_DEFAULT_METHODS,
    HAS_BEEN_REDEFINED,
    HAS_NONSTATIC_CONCRETE_METHODS,
    DECLARES_NONSTATIC_CONCRETE_METHODS,
    SHARED_LOADING_FAILED,
    IS_SHARED_BOOT_CLASS,
    IS_SHARED_PLATFORM_CLASS,
    IS_SHARED_APP_CLASS,
    HAS_CONTENDED_ANNOTATIONS,
    HAS_LOCAL_VARIABLE_TABLE,
    HAS_MIRANDA_METHODS,
    HAS_VANILLA_CONSTRUCTOR,
    HAS_FINAL_METHOD;

    init {
        bits[0] = 1 shl ordinal
    }

    fun bit(scope: Scope) =
        if (bits.size == 1) bits[0]
        else 1 shl bits[scope.version.major - 8]
}