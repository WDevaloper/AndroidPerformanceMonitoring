package com.github.crash.crash

object TestCrash {
    fun testCrash() {
        throw RuntimeException("test crash")
    }
}