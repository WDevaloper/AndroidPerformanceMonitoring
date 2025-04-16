package com.github.andcrash.jcrash;

import android.content.Context;
import android.os.Debug;

import java.io.File;
import java.io.IOException;

public class OOMUncaughtExceptionHandler implements IUncaughtExceptionHandler {
    @Override
    public void uncaughtException(Context context, String logFir, Thread thread, Throwable ex) throws IOException {
        File hprofFile = new File(logFir, "dump.hprof");// dump hprof 文件到应用的内部存储中
        Debug.dumpHprofData(hprofFile.getAbsolutePath());//调用接口获取内存快照。
    }

    @Override
    public boolean handleCrashAfter(Context context) {
        return false;
    }

    @Override
    public boolean isHandledable(Throwable ex) {
        return ex instanceof OutOfMemoryError;
    }

}
