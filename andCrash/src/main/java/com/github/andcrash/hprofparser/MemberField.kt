package com.github.andcrash.hprofparser

data class MemberField(
    val nameStringId: Long,
    val nameString: String? = null,
    val type: Int,
    val size: Int
)