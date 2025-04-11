package com.github.andcrash.jcrash;

import java.io.File;

public interface LogUploader {
    void upload(File logFile, UploadCallback callback);
}