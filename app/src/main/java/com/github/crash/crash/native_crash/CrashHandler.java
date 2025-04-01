package com.github.crash.crash.native_crash;


import android.content.Context;

import java.io.File;

public class CrashHandler {
    static {
        System.loadLibrary("nativeCrash");
    }


    public static void initCrash(Context context, String version) {
        initCrashHandler(getCrashLogPath(context), version);
    }

    private static native void initCrashHandler(String logDir, String version);

    public static native void testCrash();

    /**
     * 获取崩溃日志
     * @return logPath
     */
    public static String getCrashLogPath(Context context) {
        File crashDir = new File(context.getFilesDir().getAbsolutePath(), "crashes");
        if (!crashDir.exists() && !crashDir.mkdirs()) return "";
        return crashDir.getAbsolutePath();
    }
}