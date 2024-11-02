package land.src.toolbox.jvm.util

interface Accessible<T> {

    operator fun get(index: Int): T?

    companion object {
        fun <T> make(list: List<T>): Accessible<T> = object : Accessible<T> {
            override fun get(index: Int): T? = list.getOrNull(index)
        }
    }

}