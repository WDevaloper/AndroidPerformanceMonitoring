package com.github.crash;

import com.github.crash.crash.LogUploader;
import com.github.crash.crash.UploadCallback;

import java.io.File;

public class OkHttpUploader implements LogUploader {
    private final String uploadUrl;

    public OkHttpUploader(String url) {
        this.uploadUrl = url;
    }

    @Override
    public void upload(File logFile, UploadCallback callback) {
//        callback.onFailure(logFile, new Exception("Not implemented"));
        callback.onSuccess(logFile);
    }
}