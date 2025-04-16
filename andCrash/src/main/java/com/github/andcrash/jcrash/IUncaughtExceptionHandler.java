package com.github.andcrash.jcrash;

import android.content.Context;

import java.io.IOException;

public interface IUncaughtExceptionHandler {
    void uncaughtException(Context context, String logDir, Thread thread, Throwable ex) throws IOException;

    boolean handleCrashAfter(Context context);

    boolean isHandledable(Throwable ex);
}
