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

    public static void testNativeCrash() {
        testCrash();
    }

    private static native void testCrash();


    /**
     * 获取崩溃日志
     * @return logPath
     */
    public static String getCrashLogPath(Context context) {
        File crashDir = new File(context.getFilesDir().getAbsolutePath(), "crashes");
        if (!crashDir.exists() && !crashDir.mkdirs()) return "";
        return crashDir.getAbsolutePath();
    }

    public static void setCrashLogDir(String path) {
        setCrashLogPathNative(path);
    }

    private static native void setCrashLogPathNative(String path);
}