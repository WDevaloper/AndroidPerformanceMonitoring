package com.github.andcrash.nativecrash;


import android.content.Context;

import java.io.File;

public class NativeCrash {
    static {
        System.loadLibrary("nativeCrash");
    }


    public static void initCrash(Context context,
                                 String version,
                                 NativeCrashCallback callback) {
        initCrashHandler(getCrashLogDirectory(context), version, callback);
        File crashLogDirectory = new File(getCrashLogDirectory(context));
        if (!crashLogDirectory.exists()) {
            return;
        }
        File[] files = crashLogDirectory.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        callback.onCrashUpload(files);
    }

    private static native void initCrashHandler(String logDir, String version, NativeCrashCallback callback);


    public static void setVersion(String version) {
        SetVersion(version);
    }

    private static native void SetVersion(String version);


    public static void testNativeCrash() {
        testCrash();
    }

    private static native void testCrash();


    /**
     * 获取崩溃日志
     * @return logPath
     */
    public static String getCrashLogDirectory(Context context) {
        File crashDir = new File(context.getFilesDir(), "crash_dumps");
        if (!crashDir.exists() && !crashDir.mkdirs()) {
            throw new RuntimeException("Failed to create crash directory");
        }
        return crashDir.getAbsolutePath();
    }

    public static int deleteFile(String path) {
        return deleteCrashLogFile(path);
    }

    private static native int deleteCrashLogFile(String logPath);
}