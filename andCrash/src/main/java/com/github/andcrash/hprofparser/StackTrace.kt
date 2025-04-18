package com.github.andcrash.hprofparser

import com.github.andcrash.hprofparser.StackFrame

data class StackTrace(
    val stackTraceSerialNumber: Int,
    val threadSerialNumber: Int,
    val stackFrames: List<StackFrame>
)