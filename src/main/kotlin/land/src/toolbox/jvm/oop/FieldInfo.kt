package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.nonNullFromConstantIndex
import land.src.toolbox.jvm.dsl.nonNullFromConstantOffset
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Struct

class FieldInfo(
    val accessFlags: Short,
    val nameIndex: Short,
    val signatureIndex: Short,
    val initialValueIndex: Short,
    val lowOffset: Short,
    val highOffset: Short
)

//class FieldInfo(address: Address) : Struct(address) {
//    val accessFlags: Short by nonNullFromConstantIndex("FieldInfo::access_flags_offset")
//    val nameIndex: Short by nonNullFromConstantIndex("FieldInfo::name_index_offset")
//    val signatureIndex: Short by nonNullFromConstantIndex("FieldInfo::signature_index_offset")
//    val initialValueIndex: Short by nonNullFromConstantIndex("FieldInfo::initval_index_offset")
//    val lowOffset: Short by nonNullFromConstantIndex("FieldInfo::low_packed_offset")
//    val highOffset: Short by nonNullFromConstantIndex("FieldInfo::high_packed_offset")
//}

//class FieldInfo(address: Address) : Struct(address) {
//    val accessFlagsOffset: Int by lazy {
//        vm.constant("FieldInfo::access_flags_offset").toInt()
//    }
//
//    val nameIndexOffset: Int by lazy {
//        vm.constant("FieldInfo::name_index_offset").toInt()
//    }
//
//    val signatureIndexOffset: Int by lazy {
//        vm.constant("FieldInfo::signature_index_offset").toInt()
//    }
//
//    val initialValueIndexOffset: Int by lazy {
//        vm.constant("FieldInfo::initval_index_offset").toInt()
//    }
//
//    val lowPackedOffset: Int by lazy {
//        vm.constant("FieldInfo::low_packed_offset").toInt()
//    }
//
//    val highPackedOffset: Int by lazy {
//        vm.constant("FieldInfo::high_packed_offset").toInt()
//    }
//
//    val accessFlags: Short
//    val nameIndex: Short
//    val signatureIndex: Short
//    val initialValueIndex: Short
//    val lowOffset: Short
//    val highOffset: Short
//
//    init {
//        accessFlags = unsafe.getShort(address.base + accessFlagsOffset * Short.SIZE_BYTES)
//        nameIndex = unsafe.getShort(address.base + nameIndexOffset * Short.SIZE_BYTES)
//        signatureIndex = unsafe.getShort(address.base + signatureIndexOffset * Short.SIZE_BYTES)
//        initialValueIndex = unsafe.getShort(address.base + initialValueIndexOffset * Short.SIZE_BYTES)
//        lowOffset = unsafe.getShort(address.base + lowPackedOffset * Short.SIZE_BYTES)
//        highOffset = unsafe.getShort(address.base + highPackedOffset * Short.SIZE_BYTES)
//    }
//}