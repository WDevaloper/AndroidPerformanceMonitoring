package com.github.crash;


import android.util.Log;

import com.github.andcrash.jcrash.LogUploader;
import com.github.andcrash.jcrash.UploadCallback;

import java.io.File;

public class OkHttpUploader implements LogUploader {
    private final String uploadUrl;

    public OkHttpUploader(String url) {
        this.uploadUrl = url;
    }

    @Override
    public void upload(File logFile, UploadCallback callback) {
        Log.e("AndCrash", "upload:" + logFile.getAbsolutePath());
        callback.onSuccess(logFile);
//        callback.onFailure(logFile, new Exception("upload failed"));
    }
}