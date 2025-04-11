package com.github.andcrash.jcrash;

import java.io.File;

public interface UploadCallback {
    void onSuccess(File logFile);

    void onFailure(File logFile, Exception e);
}