package com.github.andcrash.nativecrash;

import androidx.annotation.NonNull;

import java.io.File;

public interface NativeCrashCallback {
    void onCrashReport(@NonNull String crashLogPath);

    void onCrashUpload(@NonNull File[] crashLogPath);
}