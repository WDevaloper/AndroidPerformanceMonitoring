package com.github.andcrash.hprofparser

data class ActiveThread(
    val id: Long,
    val threadSerialNumber: Int,
    val frameNumber: Int,
    val threadName: String?,
    val frames: List<ActiveThreadStackFrame>
)