package land.src.toolbox.jvm.util

import land.src.toolbox.jvm.Scope
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct
import land.src.toolbox.jvm.primitive.Type
import land.src.toolbox.util.address
import kotlin.reflect.KClass

class DynamicTypeResolver(val scope: Scope) {

    inner class VtableAccess {

        val linuxVtablePrefix by lazy {
            val lookup = scope.symbols.lookupOrNull("__vt_10JavaThread")
            if (lookup != null) {
                "__vt_"
            } else {
                "_ZTV"
            }
        }

        fun prefix(type: Type): String {
            return when (OSUtil.os) {
                OS.LINUX -> linuxVtablePrefix + type.name.length
                OS.WINDOWS -> "??_7"
                else -> error("Unsupported OS")
            }
        }

        val suffix by lazy {
            when (OSUtil.os) {
                OS.LINUX -> ""
                OS.WINDOWS -> "@@6B@"
                else -> error("Unsupported OS")
            }
        }

        fun vtableSymbol(type: Type): String {
            return prefix(type) + type.name + suffix
        }

        private fun vtableAddress0(type: Type): Long? {
            val symbol = vtableSymbol(type)
            val address = scope.symbols.lookupOrNull(symbol)
            return address?.address
        }

        val vtableAddress = makeCache(::vtableAddress0)
    }

    val vtableAccess = VtableAccess()

    /// copy of the hotspot debugger agents, addressTypeIsEqualToType
    fun addressIsForType(address: Long, type: Type): Boolean {
        val vtableAddress = vtableAccess.vtableAddress(type) ?: return false

        val addrAt = scope.unsafe.getAddress(address)

        var curType: Type? = type
        while (curType != null) {

            if (vtableAddress == addrAt) {
                return true
            }

            if (vtableAddress == addrAt - scope.pointerSize) {
                return true
            }

            if (vtableAddress == addrAt + scope.pointerSize) {
                return true
            }

            if (vtableAddress == addrAt - scope.pointerSize * 2) {
                return true
            }

            if (vtableAddress == addrAt + scope.pointerSize * 2) {
                return true
            }

            var offset = curType.size - (curType.size % scope.pointerSize)
            if (offset <= 0)
                return false

            if (vtableAddress == scope.unsafe.getAddress(address + offset)) {
                return true
            }

            offset -= scope.pointerSize
            if (offset <= 0)
                return false

            if (vtableAddress == scope.unsafe.getAddress(address + offset)) {
                return true
            }

            if (curType.superName.isBlank())
                return false

            curType = scope.vm.type(curType.superName)
        }

        return false
    }

    inline fun <reified E : Struct> constructPolymorphic(address: Address, vararg types: KClass<out E>): E? {
        for (type in types) {
            val dbType = scope.vm.vmTypes[type.simpleName!!] ?: continue
            if (addressIsForType(address.base, dbType)) {
                return type.constructors.first().call(address)
            }
        }
        return null
    }

}