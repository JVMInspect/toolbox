package land.src.jvmtb.jvm.cache

import kotlin.reflect.KClass

interface Factory {
    operator fun invoke(type: KClass<*>, address: Long = -1): Any
}