package com.github.andcrash.hprofparser

import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.Charset

private val BOOLEAN_SIZE = PrimitiveType.BOOLEAN.byteSize
private val CHAR_SIZE = PrimitiveType.CHAR.byteSize
private val BYTE_SIZE = PrimitiveType.BYTE.byteSize
private val SHORT_SIZE = PrimitiveType.SHORT.byteSize
private val INT_SIZE = PrimitiveType.INT.byteSize
private val LONG_SIZE = PrimitiveType.LONG.byteSize
private val FLOAT_SIZE = PrimitiveType.FLOAT.byteSize
private val DOUBLE_SIZE = PrimitiveType.DOUBLE.byteSize

private val BOOLEAN_TYPE = PrimitiveType.BOOLEAN.hprofType
private val CHAR_TYPE = PrimitiveType.CHAR.hprofType
private val FLOAT_TYPE = PrimitiveType.FLOAT.hprofType
private val DOUBLE_TYPE = PrimitiveType.DOUBLE.hprofType
private val BYTE_TYPE = PrimitiveType.BYTE.hprofType
private val SHORT_TYPE = PrimitiveType.SHORT.hprofType
private val INT_TYPE = PrimitiveType.INT.hprofType
private val LONG_TYPE = PrimitiveType.LONG.hprofType

private const val INT_MASK = 0xffffffffL
private const val BYTE_MASK = 0xff

fun BufferedSource.readUnsignedInt(): Long {
    return readInt().toLong() and INT_MASK
}

fun BufferedSource.readUnsignedByte(): Int {
    return readByte().toInt() and BYTE_MASK
}

fun BufferedSource.readUnsignedShort(): Int {
    return readShort().toInt() and 0xFFFF
}

fun BufferedSource.readId(identifierByteSize: Int): Long {
    // As long as we don't interpret IDs, reading signed values here is fine.
    return when (identifierByteSize) {
        1 -> readByte().toLong()
        2 -> readShort().toLong()
        4 -> readInt().toLong()
        8 -> readLong()
        else -> throw IllegalArgumentException("ID Length must be 1, 2, 4, or 8")
    }
}

fun BufferedSource.readBoolean(): Boolean {
    return readByte()
        .toInt() != 0
}

fun BufferedSource.readString(
    byteCount: Int,
    charset: Charset
): String {
    return readString(byteCount.toLong(), charset)
}

fun BufferedSource.readChar(): Char {
    return readString(CHAR_SIZE, Charsets.UTF_16BE)[0]
}

fun BufferedSource.readFloat(): Float {
    return Float.fromBits(readInt())
}

fun BufferedSource.readDouble(): Double {
    return Double.fromBits(readLong())
}

fun BufferedSource.readValue(type: Int, identifierByteSize: Int): ValueHolder {
    return when (type) {
        PrimitiveType.REFERENCE_HPROF_TYPE -> ValueHolder.ReferenceHolder(
            readId(identifierByteSize),
            identifierByteSize
        )
        BOOLEAN_TYPE -> ValueHolder.BooleanHolder(readBoolean())
        CHAR_TYPE -> ValueHolder.CharHolder(readChar())
        FLOAT_TYPE -> ValueHolder.FloatHolder(readFloat())
        DOUBLE_TYPE -> ValueHolder.DoubleHolder(readDouble())
        BYTE_TYPE -> ValueHolder.ByteHolder(readByte())
        SHORT_TYPE -> ValueHolder.ShortHolder(readShort())
        INT_TYPE -> ValueHolder.IntHolder(readInt())
        LONG_TYPE -> ValueHolder.LongHolder(readLong())
        else -> throw IllegalStateException("Unknown type $type")
    }
}

fun BufferedSource.readConstField(identifierByteSize: Int): ConstField {
    val index = readUnsignedInt().toInt()
    val type = readUnsignedByte()
    val value = readValue(
        type = type,
        identifierByteSize = identifierByteSize
    )
    return ConstField(
        index = index,
        value = value,
        size = INT_SIZE + BYTE_SIZE + value.size
    )
}

fun BufferedSource.readStaticField(identifierByteSize: Int): StaticField {
    val nameStringId = readId(identifierByteSize)
    val type = readUnsignedByte()
    val value = readValue(type = type, identifierByteSize = identifierByteSize)
    return StaticField(
        nameStringId = nameStringId,
        value = value,
        size = identifierByteSize + BYTE_SIZE + value.size
    )
}

fun BufferedSource.readMemberField(identifierByteSize: Int): MemberField {
    val id = readId(identifierByteSize)
    val type = readUnsignedByte()
    return MemberField(
        nameStringId = id,
        type = type,
        size = identifierByteSize + BYTE_SIZE
    )
}

fun BufferedSource.readStringRecord(header: HprofHeader, bodyLength: Long): HprofRecord.StringRecord {
    return HprofRecord.StringRecord(
        resId = readId(header.identifierByteSize),
        string = readUtf8((bodyLength - header.identifierByteSize)),
        bodyLength = bodyLength
    )
}

fun BufferedSource.readLoadClassRecord(
    header: HprofHeader
): HprofRecord.LoadedClassRecord {
    val classSerialNumber = readInt()
    val id = readId(header.identifierByteSize)
    val stackTraceSerialNumber = readInt()
    val classNameStrId = readId(header.identifierByteSize)
    return HprofRecord.LoadedClassRecord(
        classSerialNumber = classSerialNumber,
        id = id,
        stackTraceSerialNumber = stackTraceSerialNumber,
        classNameStrId = classNameStrId,
        bodyLength = (INT_SIZE * 2 + header.identifierByteSize * 2).toLong()
    )
}

fun BufferedSource.readUnloadClassRecord(): HprofRecord.UnloadClassRecord {
    return HprofRecord.UnloadClassRecord(
        classSerialNumber = readInt(),
        bodyLength = INT_SIZE.toLong()
    )
}

fun BufferedSource.readStackFrameRecord(
    header: HprofHeader
): HprofRecord.StackFrameRecord {
    return HprofRecord.StackFrameRecord(
        id = readId(header.identifierByteSize),
        methodNameStringId = readId(header.identifierByteSize),
        methodSignatureStringId = readId(header.identifierByteSize),
        sourceFileNameStringId = readId(header.identifierByteSize),
        classSerialNumber = readInt(),
        lineNumber = readInt(),
        bodyLength = (INT_SIZE * 2 + header.identifierByteSize * 4).toLong()
    )
}

fun BufferedSource.readStackTraceRecord(
    header: HprofHeader
): HprofRecord.StackTraceRecord {
    val stackTraceSerialNumber = readInt()
    val threadSerialNumber = readInt()
    val stackFrameIds = LongArray(readInt()) { readId(header.identifierByteSize) }
    return HprofRecord.StackTraceRecord(
        stackTraceSerialNumber = stackTraceSerialNumber,
        threadSerialNumber = threadSerialNumber,
        stackFrameIds = stackFrameIds,
        bodyLength = (INT_SIZE * 2 + header.identifierByteSize * stackFrameIds.size).toLong()
    )
}

fun BufferedSource.readRootUnknownRecord(
    header: HprofHeader
): HprofRecord.RootUnknownRecord {
    return HprofRecord.RootUnknownRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readRootJniGlobalRecord(
    header: HprofHeader
): HprofRecord.RootJniGlobalRecord {
    return HprofRecord.RootJniGlobalRecord(
        id = readId(header.identifierByteSize),
        refId = readId(header.identifierByteSize),
        bodyLength = (header.identifierByteSize * 2).toLong()
    )
}

fun BufferedSource.readRootJniLocalRecord(
    header: HprofHeader
): HprofRecord.RootJniLocalRecord {
    return HprofRecord.RootJniLocalRecord(
        id = readId(header.identifierByteSize),
        threadSerialNumber = readInt(),
        frameNumber = readInt(),
        bodyLength = (header.identifierByteSize + INT_SIZE * 2).toLong()
    )
}

fun BufferedSource.readRootJavaFrameRecord(
    header: HprofHeader
): HprofRecord.RootJavaFrameRecord {
    return HprofRecord.RootJavaFrameRecord(
        id = readId(header.identifierByteSize),
        threadSerialNumber = readInt(),
        frameNumber = readInt(),
        bodyLength = (header.identifierByteSize + INT_SIZE * 2).toLong()
    )
}

fun BufferedSource.readRootNativeStackRecord(
    header: HprofHeader
): HprofRecord.RootNativeStackRecord {
    return HprofRecord.RootNativeStackRecord(
        id = readId(header.identifierByteSize),
        threadSerialNumber = readInt(),
        bodyLength = (header.identifierByteSize + INT_SIZE).toLong()
    )
}

fun BufferedSource.readRootStickyClassRecord(
    header: HprofHeader
): HprofRecord.RootStickyClassRecord {
    return HprofRecord.RootStickyClassRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readRootThreadBlockRecord(
    header: HprofHeader
): HprofRecord.RootThreadBlockRecord {
    return HprofRecord.RootThreadBlockRecord(
        id = readId(header.identifierByteSize),
        threadSerialNumber = readInt(),
        bodyLength = (header.identifierByteSize + INT_SIZE).toLong()
    )
}

fun BufferedSource.readRootMonitorUsedRecord(
    header: HprofHeader
): HprofRecord.RootMonitorUsedRecord {
    return HprofRecord.RootMonitorUsedRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readRootThreadObjectRecord(
    header: HprofHeader
): HprofRecord.RootThreadObjectRecord {
    return HprofRecord.RootThreadObjectRecord(
        id = readId(header.identifierByteSize),
        threadSerialNumber = readInt(),
        frameNumber = readInt(),
        bodyLength = (header.identifierByteSize + INT_SIZE * 2).toLong()
    )
}

fun BufferedSource.readRootInternedStringRecord(
    header: HprofHeader
): HprofRecord.RootInternedStringRecord {
    return HprofRecord.RootInternedStringRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readRootFinalizingRecord(
    header: HprofHeader
): HprofRecord.RootFinalizingRecord {
    return HprofRecord.RootFinalizingRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readRootDebuggerRecord(
    header: HprofHeader
): HprofRecord.RootDebuggerRecord {
    return HprofRecord.RootDebuggerRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readRootReferenceCleanupRecord(
    header: HprofHeader
): HprofRecord.RootReferenceCleanupRecord {
    return HprofRecord.RootReferenceCleanupRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readRootVmInternalRecord(
    header: HprofHeader
): HprofRecord.RootVmInternalRecord {
    return HprofRecord.RootVmInternalRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readRootJniMonitorRecord(
    header: HprofHeader
): HprofRecord.RootJniMonitorRecord {
    return HprofRecord.RootJniMonitorRecord(
        id = readId(header.identifierByteSize),
        threadSerialNumber = readInt(),
        stackDepth = readInt(),
        bodyLength = (header.identifierByteSize + INT_SIZE * 2).toLong()
    )
}

fun BufferedSource.readRootUnreachableRecord(
    header: HprofHeader
): HprofRecord.RootUnreachableRecord {
    return HprofRecord.RootUnreachableRecord(
        id = readId(header.identifierByteSize),
        bodyLength = header.identifierByteSize.toLong()
    )
}

fun BufferedSource.readClassDumpRecord(
    header: HprofHeader
): HprofRecord.ClassDumpRecord {
    var bodySize = 0
    val id = readId(header.identifierByteSize)
    bodySize += INT_SIZE
    val stackTraceSerialNumber = readInt()
    bodySize += INT_SIZE
    val superClassId = readId(header.identifierByteSize)
    bodySize += header.identifierByteSize
    val classLoaderId = readId(header.identifierByteSize)
    bodySize += header.identifierByteSize
    val signersId = readId(header.identifierByteSize)
    bodySize += header.identifierByteSize
    val protectionDomainId = readId(header.identifierByteSize)
    bodySize += header.identifierByteSize
    skip(2 * header.identifierByteSize.toLong())
    bodySize += header.identifierByteSize * 2
    val instanceSize = readInt()
    bodySize += INT_SIZE
    val constPoolSize = readUnsignedShort()
    bodySize += SHORT_SIZE
    val constFields = ArrayList<ConstField>()
    repeat(constPoolSize) {
        constFields.add(readConstField(header.identifierByteSize).apply { bodySize += size })
    }
    val staticFields = ArrayList<StaticField>()
    val staticFieldSize = readUnsignedShort()
    bodySize += SHORT_SIZE
    repeat(staticFieldSize) {
        staticFields.add(readStaticField(header.identifierByteSize).apply { bodySize += size })
    }
    val memberFields = ArrayList<MemberField>()
    val memberFieldSize = readUnsignedShort()
    bodySize += SHORT_SIZE
    repeat(memberFieldSize) {
        memberFields.add(readMemberField(header.identifierByteSize).apply { bodySize += size })
    }
    return HprofRecord.ClassDumpRecord(
        id = id,
        stackTraceSerialNumber = stackTraceSerialNumber,
        superClassId = superClassId,
        classLoaderId = classLoaderId,
        signersId = signersId,
        protectionDomainId = protectionDomainId,
        instanceSize = instanceSize,
        constFields = constFields,
        staticFields = staticFields,
        memberFields = memberFields,
        bodyLength = bodySize.toLong()
    )
}

fun BufferedSource.readInstanceDumpRecord(
    header: HprofHeader
): HprofRecord.InstanceDumpRecord {
    val id = readId(header.identifierByteSize)
    val stackTraceSerialNumber = readInt()
    val classId = readId(header.identifierByteSize)
    val byteSize = readInt()
    val fieldValue = readByteArray(byteSize.toLong())
    return HprofRecord.InstanceDumpRecord(
        id = id,
        stackTraceSerialNumber = stackTraceSerialNumber,
        classId = classId,
        fieldValue = fieldValue,
        bodyLength = (header.identifierByteSize * 2 + INT_SIZE * 2 + LONG_SIZE).toLong()
    )
}

fun BufferedSource.readObjectArrayDumpRecord(
    header: HprofHeader
): HprofRecord.ObjectArrayRecord {
    val id = readId(header.identifierByteSize)
    val stackTraceSerialNumber = readInt()
    val arrayLength = readInt()
    val arrayClassId = readId(header.identifierByteSize)
    val elementIds = LongArray(arrayLength) { readId(header.identifierByteSize) }
    return HprofRecord.ObjectArrayRecord(
        id = id,
        stackTraceSerialNumber = stackTraceSerialNumber,
        arrayLength = arrayLength,
        arrayClassId = arrayClassId,
        elementIds = elementIds,
        bodyLength = (header.identifierByteSize * (2 + arrayLength) + INT_SIZE * 2).toLong()
    )
}

fun BufferedSource.readPrimitiveArrayDumpRecord(
    header: HprofHeader
): HprofRecord {
    var bodyLength: Int = 0
    val id = readId(header.identifierByteSize)
    bodyLength += header.identifierByteSize
    val stackTraceSerialNumber = readInt()
    bodyLength += INT_SIZE
    val arrayLength = readInt()
    bodyLength += INT_SIZE
    val r: HprofRecord = when(val type = readUnsignedByte()) {
        PrimitiveType.BOOLEAN.hprofType -> {
            HprofRecord.BoolArrayRecord(
                id = id,
                stackTraceSerialNumber = stackTraceSerialNumber,
                array = BooleanArray(arrayLength) { readByte().toInt() != 0 },
                bodyLength = (bodyLength + arrayLength * BOOLEAN_SIZE).toLong()
            )
        }

        PrimitiveType.CHAR.hprofType -> {
            HprofRecord.CharArrayRecord(
                id = id,
                stackTraceSerialNumber = stackTraceSerialNumber,
                array = CharArray(arrayLength) { readChar() },
                bodyLength = (bodyLength + arrayLength * CHAR_SIZE).toLong()
            )
        }

        PrimitiveType.FLOAT.hprofType -> {
            HprofRecord.FloatArrayRecord(
                id = id,
                stackTraceSerialNumber = stackTraceSerialNumber,
                array = FloatArray(arrayLength) { readFloat() },
                bodyLength = (bodyLength + arrayLength * FLOAT_SIZE).toLong()
            )
        }

        PrimitiveType.DOUBLE.hprofType -> {
            HprofRecord.DoubleArrayRecord(
                id = id,
                stackTraceSerialNumber = stackTraceSerialNumber,
                array = DoubleArray(arrayLength) { readDouble() },
                bodyLength = (bodyLength + arrayLength * DOUBLE_SIZE).toLong()
            )
        }

        PrimitiveType.BYTE.hprofType -> {
            HprofRecord.ByteArrayRecord(
                id = id,
                stackTraceSerialNumber = stackTraceSerialNumber,
                array = ByteArray(arrayLength) { readByte() },
                bodyLength = (bodyLength + arrayLength * BYTE_SIZE).toLong()
            )
        }

        PrimitiveType.SHORT.hprofType -> {
            HprofRecord.ShortArrayRecord(
                id = id,
                stackTraceSerialNumber = stackTraceSerialNumber,
                array = ShortArray(arrayLength) { readShort() },
                bodyLength = (bodyLength + arrayLength * SHORT_SIZE).toLong()
            )
        }

        PrimitiveType.INT.hprofType -> {
            HprofRecord.IntArrayRecord(
                id = id,
                stackTraceSerialNumber = stackTraceSerialNumber,
                array = IntArray(arrayLength) { readInt() },
                bodyLength = (bodyLength + arrayLength * INT_SIZE).toLong()
            )
        }

        PrimitiveType.LONG.hprofType -> {
            HprofRecord.LongArrayRecord(
                id = id,
                stackTraceSerialNumber = stackTraceSerialNumber,
                array = LongArray(arrayLength) { readLong() },
                bodyLength = (bodyLength + arrayLength * LONG_SIZE).toLong()
            )
        }

        else -> {
            throw IOException("Wrong PrimitiveType: $type")
        }
    }
    return r
}

fun BufferedSource.readPrimitiveArrayNoDataRecord(
    header: HprofHeader
): HprofRecord {
    return HprofRecord.PrimitiveArrayNoDataRecord(
        id = readId(header.identifierByteSize),
        stackTraceSerialNumber = readInt(),
        arrayLength = readInt(),
        arrayType = readUnsignedByte(),
        bodyLength = (header.identifierByteSize + 4 + 4 + 1).toLong()
    )
}

fun BufferedSource.readHeapDumpInfoRecord(
    header: HprofHeader
): HprofRecord.HeapDumpInfoRecord {
    return HprofRecord.HeapDumpInfoRecord(
        heapId = readId(header.identifierByteSize),
        stringId = readId(header.identifierByteSize),
        bodyLength = (header.identifierByteSize * 2).toLong()
    )
}

fun BufferedSource.readHeapDumpRecord(
    header: HprofHeader,
    bodyLength: Long): HprofRecord.HeapDumpRecord {
    val body = readByteArray(bodyLength)
    ByteArrayInputStream(body).source().buffer().use {
        with(it) {
            val subRecords = ArrayList<HprofRecord>()
            while (!this.exhausted()) {
                when (val subTagInt = readUnsignedByte()) {
                    HprofRecordTag.ROOT_UNKNOWN.tag -> {
                        subRecords.add(readRootUnknownRecord(header))
                    }
                    HprofRecordTag.ROOT_JNI_GLOBAL.tag -> {
                        subRecords.add(readRootJniGlobalRecord(header))
                    }
                    HprofRecordTag.ROOT_JNI_LOCAL.tag -> {
                        subRecords.add(readRootJniLocalRecord(header))
                    }
                    HprofRecordTag.ROOT_JAVA_FRAME.tag -> {
                        subRecords.add(readRootJavaFrameRecord(header))
                    }
                    HprofRecordTag.ROOT_NATIVE_STACK.tag -> {
                        subRecords.add(readRootNativeStackRecord(header))
                    }
                    HprofRecordTag.ROOT_STICKY_CLASS.tag -> {
                        subRecords.add(readRootStickyClassRecord(header))
                    }
                    HprofRecordTag.ROOT_THREAD_BLOCK.tag -> {
                        subRecords.add(readRootThreadBlockRecord(header))
                    }
                    HprofRecordTag.ROOT_MONITOR_USED.tag -> {
                        subRecords.add(readRootMonitorUsedRecord(header))
                    }
                    HprofRecordTag.ROOT_THREAD_OBJECT.tag -> {
                        subRecords.add(readRootThreadObjectRecord(header))
                    }
                    HprofRecordTag.ROOT_INTERNED_STRING.tag -> {
                        subRecords.add(readRootInternedStringRecord(header))
                    }
                    HprofRecordTag.ROOT_FINALIZING.tag -> {
                        subRecords.add(readRootFinalizingRecord(header))
                    }
                    HprofRecordTag.ROOT_DEBUGGER.tag -> {
                        subRecords.add(readRootDebuggerRecord(header))
                    }
                    HprofRecordTag.ROOT_REFERENCE_CLEANUP.tag -> {
                        subRecords.add(readRootReferenceCleanupRecord(header))
                    }

                    HprofRecordTag.ROOT_VM_INTERNAL.tag -> {
                        subRecords.add(readRootVmInternalRecord(header))
                    }

                    HprofRecordTag.ROOT_JNI_MONITOR.tag -> {
                        subRecords.add(readRootJniMonitorRecord(header))
                    }

                    HprofRecordTag.ROOT_UNREACHABLE.tag -> {
                        subRecords.add(readRootUnreachableRecord(header))
                    }

                    /**
                     * - id
                     * - stackTraceSerialNumber(int)
                     * - superClassId
                     * - classLoaderId
                     * - signersId
                     * - protectionDomainId
                     * - skip 2 * identifierSize
                     * - instanceSize(int) // in bytes
                     * - constPoolCount(short)
                     *    - index(short)
                     *    - size(byte)
                     *    - value
                     * - staticFieldCount(short)
                     *    - id
                     *    - type(byte)
                     *    - value
                     * - memberFieldCount(short)
                     *    - id
                     *    - type(byte)
                     */
                    HprofRecordTag.CLASS_DUMP.tag -> {
                        subRecords.add(readClassDumpRecord(header))
                    }

                    /**
                     * - id
                     * - stackTraceSerialNumber(int)
                     * - classId
                     * - fieldSize(int)
                     * - fieldValue
                     */
                    HprofRecordTag.INSTANCE_DUMP.tag -> {
                        subRecords.add(readInstanceDumpRecord(header))
                    }

                    /**
                     * - id
                     * - stackTraceSerialNumber(int)
                     * - arrayLength(int)
                     * - arrayClassId
                     * - elementIds (count of arrayLength)
                     */
                    HprofRecordTag.OBJECT_ARRAY_DUMP.tag -> {
                        subRecords.add(readObjectArrayDumpRecord(header))
                    }

                    /**
                     * - id
                     * - stackTraceSerialNumber(int)
                     * - arrayLength(int)
                     * - elementType(byte)
                     * - elementValues
                     */
                    HprofRecordTag.PRIMITIVE_ARRAY_DUMP.tag -> {
                        subRecords.add(readPrimitiveArrayDumpRecord(header))
                    }

                    HprofRecordTag.PRIMITIVE_ARRAY_NODATA.tag -> {
                        subRecords.add(readPrimitiveArrayNoDataRecord(header))
                    }

                    /**
                     * - heapId
                     * - stringId
                     */
                    HprofRecordTag.HEAP_DUMP_INFO.tag -> {
                        subRecords.add(readHeapDumpInfoRecord(header))
                    }

                    else -> {
                        throw IOException("Wrong subTag: $subTagInt")
                    }
                }
            }
            return HprofRecord.HeapDumpRecord(
                subRecords = subRecords,
                bodyLength = bodyLength
            )
        }
    }
}
