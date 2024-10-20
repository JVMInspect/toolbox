package land.src.jvmtb.jvm.oop

class LineNumberPair(
    val line: Short,
    val bci: Short
)

// todo port CompressedLineNumberReadStream
class CompressedLineNumberReadStream(array: Array<Short>) : Iterator<LineNumberPair> {
    override fun hasNext(): Boolean {
        TODO("Not yet implemented")
    }

    override fun next(): LineNumberPair {
        TODO("Not yet implemented")
    }
}