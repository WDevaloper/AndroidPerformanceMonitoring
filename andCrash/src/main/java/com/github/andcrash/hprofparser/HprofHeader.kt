package com.github.andcrash.hprofparser

import okio.BufferedSource
import okio.Source
import java.io.IOException
import kotlin.jvm.Throws

data class HprofHeader(
    val heapDumpTimestamp: Long,
    val version: HprofVersion,
    /**
     * Size of Hprof identifiers. Identifiers are used to represent UTF8 strings, objects,
     * stack traces, etc. They can have the same size as host pointers or sizeof(void*), but are not
     * required to be.
     */
    val identifierByteSize: Int
) {

    val headerSize: Int by lazy {
        // 版本字符串长度 + 1 （字符串结尾）+ 对象引用的占位长度 + 时间戳
        version.versionString.toByteArray(Charsets.UTF_8).size + 1 + 4 + 8
    }
}

@Throws(IOException::class)
fun Source.parseHprofHeader(): HprofHeader {
    val endOfVersionString = (this as BufferedSource).indexOf(0x00)
    val versionString = readUtf8(endOfVersionString)
    val version = HprofVersion.entries.find { it.versionString == versionString }
    if (version == null) {
        throw IOException("Unknown version: $versionString")
    }
    // skip string end
    skip(1)
    val identifierByteSize = readInt()
    val heapDumpTimestamp = readLong()
    return HprofHeader(
        heapDumpTimestamp = heapDumpTimestamp,
        version = version,
        identifierByteSize = identifierByteSize
    )
}