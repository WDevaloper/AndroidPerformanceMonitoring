package com.github.andcrash.hprofparser

import okio.IOException
import okio.buffer
import okio.source
import java.io.File
import kotlin.jvm.Throws

data class Hprof(
    val header: HprofHeader,
    val records: Map<Class<out HprofRecord>, List<HprofRecord>>,
    val recordsLinked: HprofRecordsLinked,
    val refTree: RefTree
)


@Suppress("UNCHECKED_CAST")
@Throws(IOException::class)
fun hprofParse(filePath: String): Hprof {
    val f = File(filePath)
    val (header, records) = f.source().buffer().use {
        val header = it.parseHprofHeader()
        val records = it.parseHprofRecords(header)
        header to records
    }
    val linked = linkRecords(records as Map<Class<*>, List<HprofRecord>>, header)
    val refTree = createGcRootRefGraph(linked)
    return Hprof(
        header = header,
        records = records,
        recordsLinked = linked,
        refTree = refTree
    )
}