package com.github.andcrash.jcrash;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class OOMUncaughtExceptionHandler implements IUncaughtExceptionHandler {
    @Override
    public boolean uncaughtException(Context context, String logFir, Thread thread, Throwable ex) {
        if (ex instanceof OutOfMemoryError) {
            try {
                File hprofFile = new File(logFir, "dump.hprof");// dump hprof 文件到应用的内部存储中
                Debug.dumpHprofData(hprofFile.getAbsolutePath());//调用接口获取内存快照。
            } catch (IOException e) {
                Log.e(AndCrash.TAG, "Failed to dump hprof file", e);
            }
            return true;
        }
        return false;
    }
}
