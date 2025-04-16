package com.github.crash;

import android.content.Context;
import android.util.Log;

import com.github.andcrash.jcrash.AndCrash;
import com.github.andcrash.jcrash.IUncaughtExceptionHandler;

import java.io.IOException;

public class CustomUncaughtExceptionHandler implements IUncaughtExceptionHandler {
    @Override
    public void uncaughtException(Context context, String logDir, Thread thread, Throwable ex) throws IOException {
        CustomException customException = (CustomException) ex;
        customException.printStackTrace();
        Log.e(AndCrash.TAG, "CustomException: " + customException.getMessage());
    }

    @Override
    public boolean handleCrashAfter(Context context) {
        return true;
    }

    @Override
    public boolean isHandledable(Throwable ex) {
        return ex instanceof CustomException;
    }
}
