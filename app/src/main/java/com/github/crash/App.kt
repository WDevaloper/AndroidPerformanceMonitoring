package com.github.crash

import android.app.Application
import android.util.Log
import com.github.andcrash.jcrash.CrashLogger
import com.github.andcrash.nativecrash.NativeCrash
import com.github.andcrash.nativecrash.NativeCrashCallback
import java.io.File


class App : Application(), NativeCrashCallback {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.getInstance()
            .setUploader(OkHttpUploader("https://api.example.com/crash_logs"))
            .setRetentionDays(3)
            .initialize(this)

        NativeCrash.initCrash(this, "1.0.0", this)


        val andAPM = AndAPM()
        andAPM.init()
        andAPM.start()
        andAPM.stop()
        andAPM.destroy()
    }

    /**
     * 崩溃时回调
     */
    override fun onCrashReport(crashLogPath: String) {
        Log.d("AndCrash", "onCrashReport: $crashLogPath")
    }

    /**
     * 初始化完成时回调，可用于上传Native crash日志
     */
    override fun onCrashUpload(crashLogPath: Array<out File>) {
        crashLogPath.forEach {
            if (it.delete()) {
                Log.d("AndCrash", "delete file:" + it.absolutePath)
            }
        }
    }
}