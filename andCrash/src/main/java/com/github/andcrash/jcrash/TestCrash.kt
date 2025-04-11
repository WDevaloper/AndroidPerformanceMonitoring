package com.github.andcrash.jcrash

object TestCrash {
    fun testCrash() {
        throw RuntimeException("test crash")
    }
}