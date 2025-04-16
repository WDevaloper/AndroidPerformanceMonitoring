package com.github.andcrash.jcrash;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Debug;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressLint("StaticFieldLeak")
public final class AndCrash implements Thread.UncaughtExceptionHandler {
    public static final String TAG = "AndCrash";
    private Context context;
    private LogUploader uploader;
    private long retentionDays = 7;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Thread.UncaughtExceptionHandler defaultHandler;
    private String logDir;
    private final List<IUncaughtExceptionHandler> crashHandlers = new CopyOnWriteArrayList<>();

    private static final class InstanceHolder {
        private static final AndCrash instance = new AndCrash();
    }

    private AndCrash() {
        setupExceptionHandler();
    }

    // 设置异常处理器
    private void setupExceptionHandler() {
        this.crashHandlers.add(new NormalUncaughtException());
        this.crashHandlers.add(new OOMUncaughtExceptionHandler());
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }


    public static AndCrash getInstance() {
        return InstanceHolder.instance;
    }

    public AndCrash initialize(Context context) {
        this.context = context;
        executor.execute(this::uploadPendingLogs);
        executor.execute(this::cleanExpiredLogs);
        this.getLogOrCreateDirectory();
        return this;
    }

    public AndCrash setRetentionDays(long days) {
        this.retentionDays = days;
        return this;
    }

    public AndCrash setLogDir(String logDir) {
        this.logDir = logDir;
        return this;
    }

    public AndCrash setExecutor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    public AndCrash setUploader(LogUploader uploader) {
        this.uploader = uploader;
        return this;
    }


    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        this.executor.execute(() -> saveCrashLog(ex, thread, () -> subsequentProcessing(thread, ex)));
    }

    // 后续处理
    private void subsequentProcessing(Thread thread, Throwable ex) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Process.killProcess(Process.myPid());
        System.exit(1);

        //使用系统默认方式处理异常
        defaultHandler.uncaughtException(thread, ex);
    }

    // 保存崩溃日志
    private void saveCrashLog(Throwable ex, Thread thread, Runnable runnable) {
        for (IUncaughtExceptionHandler handler : crashHandlers) {
            boolean uncaughted = handler.uncaughtException(context, logDir, thread, ex);
            if (uncaughted) break;
        }
        runnable.run();
    }

    public void postCrash(Throwable ex) {
        this.executor.execute(() -> saveCrashLog(ex, Thread.currentThread(),
                () -> Log.d(TAG, " Post crash log saved")));
    }


    // 清理过期日志
    private void cleanExpiredLogs() {
        File[] logs = getLogOrCreateDirectory().listFiles();
        if (logs == null) return;
        long cutoff = System.currentTimeMillis() - (retentionDays * 86400000L);
        for (File log : logs) {
            if (log.lastModified() < cutoff) {
                String name = log.getName();
                boolean deleteResult = log.delete();
                Log.w(TAG, "Delete expired crash log delete result: " + deleteResult + " name: " + name);
            }
        }
    }


    //上传未上传的日志
    public void uploadPendingLogs() {
        if (uploader == null || TextUtils.isEmpty(this.logDir)) return;
        File[] logs = getLogOrCreateDirectory().listFiles((dir, name) -> name.endsWith(".log"));
        if (logs != null && logs.length > 0) {
            uploadPendingLogs(logs);
            return;
        }
        Log.d(TAG, "uploadPendingLogs log file is null");
    }

    private void uploadPendingLogs(File[] logs) {
        for (File log : logs) {
            Log.d(TAG, "Upload pending log: " + log.getName());
            uploader.upload(log, new UploadCallback() {
                @Override
                public void onSuccess(File file) {
                    if (file.exists()) {
                        boolean deleteResult = file.delete();
                        Log.d(TAG, "Delete result: " + deleteResult + " name: " + file.getName());
                    }
                }

                @Override
                public void onFailure(File file, Exception e) {
                    Log.w(TAG, "Upload failed: " + file.getName());
                }
            });
        }
    }


    private File getLogOrCreateDirectory() {
        if (this.logDir != null) return new File(this.logDir);
        File externalDir = context.getFilesDir();
        File logDir = new File(externalDir, "crash_logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            Log.d(TAG, "Failed to create log directory");
        }
        this.logDir = logDir.getAbsolutePath();
        return logDir;
    }
}