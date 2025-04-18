package com.github.andcrash.hprofparser

import okio.BufferedSource
import java.io.IOException
import kotlin.jvm.Throws

sealed class HprofRecord {

    abstract val bodyLength: Long

    data class StringRecord(
        val resId: Long,
        val string: String,
        override val bodyLength: Long
    ) : HprofRecord()

    data class LoadedClassRecord(
        val classSerialNumber: Int,
        val id: Long,
        val stackTraceSerialNumber: Int,
        val classNameStrId: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class UnloadClassRecord(
        val classSerialNumber: Int,
        override val bodyLength: Long
    ) : HprofRecord()

    data class StackFrameRecord(
        val id: Long,
        val methodNameStringId: Long,
        val methodSignatureStringId: Long,
        val sourceFileNameStringId: Long,
        val classSerialNumber: Int,
        val lineNumber: Int,
        override val bodyLength: Long
    ) : HprofRecord()

    data class StackTraceRecord(
        val stackTraceSerialNumber: Int,
        val threadSerialNumber: Int,
        val stackFrameIds: LongArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootUnknownRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootJniGlobalRecord(
        val id: Long,
        val refId: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootJniLocalRecord(
        val id: Long,
        val threadSerialNumber: Int,
        val frameNumber: Int,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootJavaFrameRecord(
        val id: Long,
        val threadSerialNumber: Int,
        val frameNumber: Int,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootNativeStackRecord(
        val id: Long,
        val threadSerialNumber: Int,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootStickyClassRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootThreadBlockRecord(
        val id: Long,
        val threadSerialNumber: Int,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootMonitorUsedRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootThreadObjectRecord(
        val id: Long,
        val threadSerialNumber: Int,
        val frameNumber: Int,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootInternedStringRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootFinalizingRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootDebuggerRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootReferenceCleanupRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootVmInternalRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootJniMonitorRecord(
        val id: Long,
        val threadSerialNumber: Int,
        val stackDepth: Int,
        override val bodyLength: Long
    ) : HprofRecord()

    data class RootUnreachableRecord(
        val id: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data class HeapDumpRecord(
        val subRecords: List<HprofRecord>,
        override val bodyLength: Long
    ) : HprofRecord()

    data class ClassDumpRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val superClassId: Long,
        val classLoaderId: Long,
        val signersId: Long,
        val protectionDomainId: Long,
        // in bytes
        val instanceSize: Int,
        val constFields: List<ConstField>,
        val staticFields: List<StaticField>,
        val memberFields: List<MemberField>,
        override val bodyLength: Long
    ) : HprofRecord()


    data class InstanceDumpRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val classId: Long,
        val fieldValue: ByteArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class ObjectArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val arrayLength: Int,
        val arrayClassId: Long,
        val elementIds: LongArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class BoolArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val array: BooleanArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class CharArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val array: CharArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class FloatArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val array: FloatArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class DoubleArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val array: DoubleArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class ByteArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val array: ByteArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class ShortArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val array: ShortArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class IntArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val array: IntArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class LongArrayRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val array: LongArray,
        override val bodyLength: Long
    ) : HprofRecord()

    data class PrimitiveArrayNoDataRecord(
        val id: Long,
        val stackTraceSerialNumber: Int,
        val arrayLength: Int,
        val arrayType: Int,
        override val bodyLength: Long,
    ) : HprofRecord()

    data class HeapDumpInfoRecord(
        val heapId: Long,
        val stringId: Long,
        override val bodyLength: Long
    ) : HprofRecord()

    data object HeapDumpEnd : HprofRecord() {
        override val bodyLength: Long = 0
    }

    data class UnknownRecord(
        val tag: HprofRecordTag?,
        val timeStamp: Long,
        override val bodyLength: Long,
        val body: ByteArray
    ) : HprofRecord()
}

@Throws(IOException::class)
fun BufferedSource.parseHprofRecords(header: HprofHeader): Map<Class<out HprofRecord>, List<HprofRecord>> {

    val strings = ArrayList<HprofRecord.StringRecord>()
    val loadClasses = ArrayList<HprofRecord.LoadedClassRecord>()
    val unloadClasses = ArrayList<HprofRecord.UnloadClassRecord>()
    val stackFrames = ArrayList<HprofRecord.StackFrameRecord>()
    val stackTraces = ArrayList<HprofRecord.StackTraceRecord>()

    val heapDumpRecord = ArrayList<HprofRecord.HeapDumpRecord>()

    val heapDumpEnd = ArrayList<HprofRecord.HeapDumpEnd>()

    val unknown = ArrayList<HprofRecord.UnknownRecord>()


    val ret = HashMap<Class<out HprofRecord>, List<HprofRecord>>()

    ret[HprofRecord.StringRecord::class.java] = strings
    ret[HprofRecord.LoadedClassRecord::class.java] = loadClasses
    ret[HprofRecord.UnloadClassRecord::class.java] = unloadClasses
    ret[HprofRecord.StackFrameRecord::class.java] = stackFrames
    ret[HprofRecord.StackTraceRecord::class.java] = stackTraces
    ret[HprofRecord.HeapDumpRecord::class.java] = heapDumpRecord
    ret[HprofRecord.HeapDumpEnd::class.java] = heapDumpEnd
    ret[HprofRecord.UnknownRecord::class.java] = unknown

    while (!exhausted()) {
        val tagInt = this.readUnsignedByte()
        val timeStamp = this.readUnsignedInt()
        val bodyLength = this.readUnsignedInt()

        when (tagInt) {

            /**
             * - resId
             * - string body
             */
            HprofRecordTag.STRING_IN_UTF8.tag -> {
                val sr = readStringRecord(header, bodyLength)
                strings.add(sr)
            }

            /**
             * - classSerialNumber(Int)
             * - id
             * - stackTraceSerialNumber(Int)
             * - classNameStringId
             */
            HprofRecordTag.LOAD_CLASS.tag -> {
                val r = readLoadClassRecord(header)
                loadClasses.add(r)
            }

            /**
             * classSerialNumber(Int)
             */
            HprofRecordTag.UNLOAD_CLASS.tag -> {
                unloadClasses.add(readUnloadClassRecord())
            }

            HprofRecordTag.STACK_FRAME.tag -> {
                stackFrames.add(readStackFrameRecord(header))
            }

            HprofRecordTag.STACK_TRACE.tag -> {
                stackTraces.add(readStackTraceRecord(header))
            }

            HprofRecordTag.HEAP_DUMP.tag, HprofRecordTag.HEAP_DUMP_SEGMENT.tag -> {
                heapDumpRecord.add(readHeapDumpRecord(header, bodyLength))
            }

            HprofRecordTag.HEAP_DUMP_END.tag -> {
                heapDumpEnd.add(HprofRecord.HeapDumpEnd)
            }

            else -> {
                val body = readByteArray(bodyLength)
                unknown.add(
                    HprofRecord.UnknownRecord(
                        tag = HprofRecordTag.entries.find { it.tag == tagInt },
                        timeStamp = timeStamp,
                        bodyLength = bodyLength,
                        body = body
                    )
                )
            }
        }
    }
    return ret
}