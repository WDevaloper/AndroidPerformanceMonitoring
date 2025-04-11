package com.github.andcrash.nativecrash;

public interface NativeCrashCallback {
    void onCrashReport(String crashLogPath);
}