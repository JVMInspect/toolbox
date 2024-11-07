package land.src.jvm.api

interface NArray<out E> : Iterable<E> {
    val length: Int
    val bytes: ByteArray

    operator fun get(index: Int): E?
}

interface NPrimitiveArray<E> : NArray<E> {
    override operator fun get(index: Int): E
}