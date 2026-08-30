package land.src.toolbox.jvm.oop

import land.src.toolbox.jvm.dsl.*
import land.src.toolbox.jvm.primitive.Address
import land.src.toolbox.jvm.primitive.Array
import land.src.toolbox.jvm.util.*

class InstanceKlass(address: Address) : Klass(address) {
    val majorVersion: Short get() = constantPool.majorVersion
    val minorVersion: Short get() = constantPool.minorVersion
    var constantPool: ConstantPool by nonNull("_constants")
    var initState: Byte by nonNull("_init_state")

    var superClass: Klass? by maybeNull("_super")

    val nestHostIndex: Short by lazy {
        val staticOopFieldCountAddress = address.base + type.field("_static_oop_field_count")!!.offsetOrAddress
        unsafe.getShort(staticOopFieldCountAddress - Short.SIZE_BYTES * 2)
    }

    val nestMembers: Array<Short> by lazy {
        val nestMembersAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + pointerSize
        arrays(nestMembersAddress, false)!!
    }

    val nestHost: InstanceKlass? by lazy {
        val nestHostAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + (pointerSize * 2)
        if (unsafe.getAddress(nestHostAddress) == 0L)
            return@lazy null
        InstanceKlass(Address(this, nestHostAddress))
    }

    val permittedSubclasses: Array<Short> by lazy {
        val nestHostAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + (pointerSize * 2)
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        arrays(permittedSubclassesAddress, false)!!
    }

    val recordComponents: Array<RecordComponent>? by lazy {
        val nestHostAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + (pointerSize * 2)
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        val recordComponentsAddress = permittedSubclassesAddress + pointerSize
        if (unsafe.getAddress(recordComponentsAddress) == 0L)
            return@lazy null
        arrays(recordComponentsAddress, true)
    }

    val sourceDebugExtension: String? by lazy {
        val nestHostAddress = address.base + type.field("_inner_classes")!!.offsetOrAddress + (pointerSize * 2)
        val permittedSubclassesAddress = nestHostAddress + pointerSize
        val recordComponentsAddress = permittedSubclassesAddress + pointerSize
        val sourceDebugExtensionAddress = recordComponentsAddress + pointerSize
        if (unsafe.getAddress(sourceDebugExtensionAddress) == 0L)
            return@lazy null
        unsafe.getString(unsafe.getAddress(sourceDebugExtensionAddress))
    }

    val sourceFileNameIndex: Short get() = constantPool.sourceFileNameIndex
    val genericSignatureIndex: Short get() = constantPool.genericSignatureIndex

    val annotations: Annotations? by maybeNull("_annotations")

    val innerClasses: Array<Short>? by maybeNullArray("_inner_classes")
    val localInterfaces: Array<InstanceKlass>? by maybeNullArray("_local_interfaces")

    val methods: Array<Method> by nonNullArray("_methods")
    val fieldData: Array<Byte> by nonNullArray("_fieldinfo_stream")
    var methodOrdering: Array<Int>? by maybeNullArray("_method_ordering")

    val accessFlagsOffset: Int by constant("FieldInfo::access_flags_offset")
    val nameIndexOffset: Int by constant("FieldInfo::name_index_offset")
    val signatureIndexOffset: Int by constant("FieldInfo::signature_index_offset")
    val initialValueIndexOffset: Int by constant("FieldInfo::initval_index_offset")
    val lowPackedOffset: Int by constant("FieldInfo::low_packed_offset")
    val highPackedOffset: Int by constant("FieldInfo::high_packed_offset")
    val miscFlags: Short by nonNull("_misc_flags")

    var methodsJMethodsIds: Long by nonNull("_methods_jmethod_ids")
    var osrNMethodsHead: NMethod? by maybeNull("_osr_nmethods_head")

    fun isFlagSet(flag: InstanceKlassFlag): Boolean {
        return (miscFlags.toInt() and flag.bit(this)) != 0
    }

    fun findMethod(name: String, descriptor: String): Method? {
        return methods.firstOrNull {
            val methodName = constantPool.getString(it.constMethod.nameIndex.toInt())
            val methodDescriptor = constantPool.getString(it.constMethod.signatureIndex.toInt())
            methodName == name && methodDescriptor == descriptor
        }
    }

    fun getFieldName(info: FieldInfo): String {
        return if (info.accessFlags.toInt() and JVM_ACC_FIELD_INTERNAL != 0) {
            globals.vmSymbols.lookupSymbol(info.nameIndex.toInt()).string
        } else {
            constantPool.getString(info.nameIndex.toInt())
        }
    }

    fun getFieldDescriptor(info: FieldInfo): String {
        return if (info.accessFlags.toInt() and JVM_ACC_FIELD_INTERNAL != 0) {
            globals.vmSymbols.lookupSymbol(info.signatureIndex.toInt()).string
        } else {
            constantPool.getString(info.signatureIndex.toInt())
        }
    }

    fun getField(name: String): FieldInfo {
        return fieldInfos.first { getFieldName(it) == name }
    }

    class Unsigned5Stream(val bytes: ByteArray) {
        /*
            const OFF pos = offset_rw;
            STATIC_ASSERT(sizeof(get(array, pos)) == 1);  // must be a byte-getter
            const uint32_t b_0 = (uint8_t) get(array, pos);  //b_0 = a[0]
            assert(b_0 >= X, "avoid excluded bytes");
            uint32_t sum = b_0 - X;
            if (sum < L) {  // common case
              offset_rw = pos + 1;
              return sum;
            }
            // must collect more bytes:  b[1]...b[4]
            int lg_H_i = lg_H;  // lg(H)*i == lg(H^^i)
            for (int i = 1; ; i++) {  // for i in [1..4]
              assert(limit == 0 || pos + i < limit, "oob");
              const uint32_t b_i = (uint8_t) get(array, pos + i);  //b_i = a[i]
              assert(b_i >= X, "avoid excluded bytes");
              sum += (b_i - X) << lg_H_i;  // sum += (b[i]-X)*(64^^i)
              if (b_i < X+L || i == MAX_LENGTH-1) {
                offset_rw = pos + i + 1;
                return sum;
              }
              lg_H_i += lg_H;
            }
         */
        var position: Int = 0
        fun read(): Int {
            val b0 = bytes[position++].toInt() and 0xFF
            var sum = b0 - 1
            if (sum < 191) {
                return sum
            }
            var lg_H_i = 6
            for (i in 1..4) {
                val bi = bytes[position++].toInt() and 0xFF
                sum += (bi - 1) shl lg_H_i
                if (bi < 192 || i == 4) {
                    return sum
                }
                lg_H_i += 6
            }
            return sum
        }

        fun hasNext(): Boolean {
            /*
  template<typename ARR, typename OFF, typename GET = ArrayGetSet<ARR,OFF>>
  static int check_length(ARR array, OFF offset, OFF limit = 0,
                          GET get = GET()) {
    const OFF pos = offset;
    STATIC_ASSERT(sizeof(get(array, pos)) == 1);  // must be a byte-getter
    const uint32_t b_0 = (uint8_t) get(array, pos);  //b_0 = a[0]
    if (b_0 < X+L) {
      return (b_0 < X) ? 0 : 1;
    }
    // parse more bytes:  b[1]...b[4]
    for (int i = 1; ; i++) {  // for i in [1..4]
      if (limit != 0 && pos + i >= limit)  return 0;  // limit failure
      const uint32_t b_i = (uint8_t) get(array, pos + i);  //b_i = a[i]
      if (b_i < X)  return 0;  // excluded byte found
      if (b_i < X+L || i == MAX_LENGTH-1) {
        return i + 1;
      }
    }
  }

      int next_length() {
      return UNSIGNED5::check_length(_array, _position, _limit, GET());
    }

        bool has_next() {
      return next_length() != 0;
    }

             */
            val b0 = bytes[position].toInt() and 0xFF
            if (b0 < 192) {
                return b0 > 1
            }
            for (i in 1..4) {
                val bi = bytes[position + i].toInt() and 0xFF
                if (bi < 128) return false
                if (bi < 192 || i == 4) {
                    return true
                }
            }
            return false
        }
    }

    val fieldInfos: List<FieldInfo> get() {
        val info = mutableListOf<FieldInfo>()

        val stream = Unsigned5Stream(fieldData.bytes)
        val total = stream.read() + stream.read()

        for (i in 0 until total) {
            val nameIndex = stream.read().toShort()
            val signatureIndex = stream.read().toShort()
            val offset = stream.read()
            val accessFlags = stream.read().toShort()
            val flags = stream.read()
            var initialValueIndex: Short = 0
            // flags is 5 bits representing the following:
            /*
              _ff_initialized,  // has ConstantValue initializer attribute
              _ff_injected,     // internal field injected by the JVM
              _ff_generic,      // has a generic signature
              _ff_stable,       // trust as stable b/c declared as @Stable
              _ff_contended,    // is contended, may have contention-group
            */
            if (flags and 0x1 != 0) {
                initialValueIndex = stream.read().toShort()
            }
            if (flags and 0x2 != 0) {
                stream.read()
            }
            if (flags and 0x8 != 0) {
                stream.read()
            }

            info += FieldInfo(
                accessFlags,
                nameIndex,
                signatureIndex,
                offset,
                initialValueIndex
            )
        }

        return info
    }

    fun updateFieldInfo(index: Int, info: FieldInfo) {
        val offset = index * 6
    }

    val javaFieldInfos: List<FieldInfo> by lazy {
        fieldInfos.filter { it.accessFlags.toInt() and JVM_ACC_FIELD_INTERNAL == 0 }
    }

    val resolvedFields: List<Field> by lazy {
        fieldInfos.map {
            val name = getFieldName(it)
            val descriptor = getFieldDescriptor(it)
            val offset = it.offset
            Field(name, descriptor, it.accessFlags.toInt(), offset shr 2)
        }
    }

    fun sortMethods() {
        val sorted = methods.sortedBy { it.constMethod.nameSymbol.base }

        val methodOrdering: Array<Int> = arrays.allocate0(sorted.size)

        for (i in sorted.indices) {
            val method = sorted[i]
            method.constMethod.idNum = i.toShort()

            methodOrdering[i] = i
            method.vtableIndex = -4

            methods[i] = method
        }

        this.methodOrdering = methodOrdering
    }

    fun removeOsrNMethod(n: NMethod): Boolean {
        var found = false

        var cur = osrNMethodsHead
        var last: NMethod? = null

        val m = n.method!!

        while(cur != null && cur.base != n.base) {
            last = cur
            cur = cur.osrLink
        }

        var next: NMethod? = null
        if (cur != null && cur.base == n.base) {
            found = true
            next = cur.osrLink
            if (last == null) {
                osrNMethodsHead = next
            } else {
                last.osrLink = next
            }
        }
        n.osrLink = null

        return found
    }

    fun objectHeaderSize(): Int {
        return if (globals.compressedKlassPointers.isCompressedKlassPointers()) {
            OopDesc(placeholder).klassGap.toInt()
        } else {
            structs.sizeof(OopDesc::class)
        }
    }

    fun computeSizes(): Pair<List<Int>, Int> {
        if (layoutHelper != 0) {
            return listOf<Int>() to ((layoutHelper shr 3) * 8)
        }
        var beginning = superClass?.instanceKlass?.computeSizes()?.second ?: objectHeaderSize()
        val fieldOffsets = mutableListOf<Int>()
        fieldInfos.forEach { field ->
            var fieldSize = sizeForType(this, constantPool.getString(field.signatureIndex.toInt()))

            val offset = beginning
            fieldOffsets += offset

            beginning += fieldSize
        }

        return fieldOffsets to beginning
    }

    /// Completely unlink InstanceKlass from the jvm internal references
    fun unlink() {
        // we have to remove ourselves from alot of places:
        // - SystemDictionary
        // - SymbolTable
        // - ProtectionDomainCache
        // - FinalizersCache
        // - ClassLoaderDataGraph
        // - ClassLoaderData
        // - InitializationErrorTable
    }
}

