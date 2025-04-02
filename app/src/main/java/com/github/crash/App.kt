package com.github.crash

import android.app.Application
import android.util.Log
import com.github.crash.crash.CrashLogger
import com.github.crash.crash.cnative.NativeCrash
import com.github.crash.crash.cnative.NativeCrashCallback
import java.io.File


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.getInstance()
            .setUploader(OkHttpUploader("https://api.example.com/crash_logs"))
            .setRetentionDays(3)
            .initialize(this)
        NativeCrash.initCrash(this, "1.0.0")
    }
}