package com.github.crash.crash;

import java.io.File;

public interface LogUploader {
    void upload(File logFile, UploadCallback callback);
}