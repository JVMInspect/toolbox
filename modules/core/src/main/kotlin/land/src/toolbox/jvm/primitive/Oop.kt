package land.src.toolbox.jvm.primitive

import land.src.jvm.api.Addressable
import land.src.toolbox.jvm.Scope

/**
 * Oop are [Struct] types which are cached by address.
 *
 * They must inherit [Struct] in order to be initialized and cached.
 */
interface Oop : Scope, Addressable