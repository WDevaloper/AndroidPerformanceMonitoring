package com.github.crash.crash.cnative;


import android.content.Context;
import android.util.Log;

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

    public static void onCrashReported(String logPath) {
        new Thread(() -> {
            // 仅执行轻量级操作
            Log.i("NativeCrash", "崩溃日志已生成: " + logPath);
            // 可在此处上传日志或通知用户
        }).start();
    }
}