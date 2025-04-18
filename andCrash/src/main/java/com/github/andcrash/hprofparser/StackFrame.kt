package com.github.andcrash.hprofparser

import com.github.andcrash.hprofparser.LoadedClass

data class StackFrame(
    val id: Long,
    val methodName: String?,
    val methodSignature: String?,
    val sourceFileName: String?,
    val clazz: LoadedClass?,
    val lineNumber: Int
)