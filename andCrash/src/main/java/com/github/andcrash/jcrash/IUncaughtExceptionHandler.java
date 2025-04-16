package com.github.andcrash.jcrash;

import android.content.Context;

public interface IUncaughtExceptionHandler {
    boolean uncaughtException(Context context, String logDir, Thread thread, Throwable ex);
}
