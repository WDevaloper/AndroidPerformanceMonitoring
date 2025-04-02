package com.github.crash.crash.cnative;


import android.content.Context;

import java.io.File;

public class NativeCrash {
    static {
        System.loadLibrary("nativeCrash");
    }


    public static void initCrash(Context context, String version) {
        initCrashHandler(getCrashLogPath(context), version);
    }

    private static native void initCrashHandler(String logDir, String version);


    public static void setVersion(String version) {
        SetVersion(version);
    }

    private static native void SetVersion(String version);


    public static void cleanup() {
        nativeCrashCleanup();
    }

    private static native void nativeCrashCleanup();

    public static void testNativeCrash() {
        testCrash();
    }

    private static native void testCrash();


    /**
     * 获取崩溃日志
     * @return logPath
     */
    public static String getCrashLogPath(Context context) {
        File crashDir = new File(context.getFilesDir(), "crashes");
        if (!crashDir.exists() && !crashDir.mkdirs()) {
            throw new RuntimeException("Failed to create crash directory");
        }
        return crashDir.getAbsolutePath();
    }
}