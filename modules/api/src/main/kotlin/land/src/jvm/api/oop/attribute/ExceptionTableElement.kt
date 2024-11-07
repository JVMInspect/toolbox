package land.src.jvm.api.oop.attribute

interface ExceptionTableElement {
    val end: Short
    val start: Short
    val handler: Short
    val typeIndex: Short
}