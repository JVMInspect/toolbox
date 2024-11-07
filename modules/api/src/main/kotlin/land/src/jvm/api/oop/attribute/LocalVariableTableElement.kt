package land.src.jvm.api.oop.attribute

interface LocalVariableTableElement {
    val slot: Short
    val length: Short
    val startBci: Short
    val nameIndex: Short
    val signatureIndex: Short
    val descriptorIndex: Short
}