package com.github.andcrash.jcrash;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Debug;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AndCrash implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "AndCrash";
    private Context context;
    private LogUploader uploader;
    private long retentionDays = 7;
    private final DeviceInfoCollector deviceInfoCollector;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    private AndCrash() {
        this.deviceInfoCollector = new DeviceInfoCollector();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        setupExceptionHandler();
        executor.execute(this::cleanExpiredLogs);
    }

    private static final class InstanceHolder {
        @SuppressLint("StaticFieldLeak")
        private static final AndCrash instance = new AndCrash();
    }

    public static AndCrash getInstance() {
        return InstanceHolder.instance;
    }

    public AndCrash initialize(Context context) {
        this.context = context;
        uploadPendingLogs();
        return this;
    }

    public AndCrash setRetentionDays(long days) {
        this.retentionDays = days;
        return this;
    }

    public AndCrash setUploader(LogUploader uploader) {
        this.uploader = uploader;
        return this;
    }

    public AndCrash newInstance() {
        return new AndCrash()
                .setUploader(this.uploader)
                .setRetentionDays(this.retentionDays)
                .initialize(context);
    }


    // 设置异常处理器
    private void setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(this);
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
        Log.d(TAG, "Crash log saved: " + ex.getMessage());
        if (ex instanceof OutOfMemoryError) {
            try {
                // dump hprof 文件到应用的内部存储中
                File hprofFile = new File(context.getFilesDir(), "dump.hprof");
                //调用接口获取内存快照。
                Debug.dumpHprofData(hprofFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Failed to dump hprof file", e);
                e.printStackTrace();
            }
            return;
        }

        long startTime = System.currentTimeMillis();
        Log.d(TAG, "Crash log saved: -----");
        byte[] logContent =
                deviceInfoCollector.buildLogContent(this.context, ex, thread).getBytes();
        Log.d(TAG, "Crash log saved: " + new String(logContent));
        //可以固定map大小，也可以通过数据计算
        long dataLength = logContent.length;
        File logFile = createLogFile();
        Log.d(TAG, "Crash log saved: " + dataLength);
        try (RandomAccessFile raf =
                     new RandomAccessFile(logFile, "rw");
             FileChannel channel = raf.getChannel()) {
            MappedByteBuffer buffer =
                    channel.map(FileChannel.MapMode.READ_WRITE, 0, dataLength);
            buffer.put(logContent);
            buffer.force();

            // 显式清理 MappedByteBuffer 资源
            mappedByteBufferCleaner(buffer);

            Log.d(TAG, "Crash log saved: " + logFile.getName());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save crash log", e);
        } finally {
            Log.d(TAG, "Crash log saved in " + (System.currentTimeMillis() - startTime) + "ms");
            runnable.run();
        }
    }

    public void postCrash(Throwable ex) {
        this.executor.execute(() -> saveCrashLog(ex, Thread.currentThread(),
                () -> Log.d(TAG, " Post crash log saved")));
    }

    // 显式清理 MappedByteBuffer 资源
    private void mappedByteBufferCleaner(MappedByteBuffer buffer) {
        try {
            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);
            if (cleaner != null) {
                cleaner.getClass().getMethod("clean").invoke(cleaner);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to clean MappedByteBuffer resources", e);
        }
    }

    // 清理过期日志
    private void cleanExpiredLogs() {
        File[] logs = getLogDirectory().listFiles();
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

    // 构建崩溃日志

    // 创建崩溃日志文件
    private File createLogFile() {
        File logDir = getLogDirectory();
        String fileName = "crash_" + System.currentTimeMillis() + ".log";
        return new File(logDir, fileName);
    }

    private File getLogDirectory() {
        File externalDir = context.getFilesDir();
        File logDir = new File(externalDir, "crash_logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            Log.e(TAG, "Failed to create log directory");
        }
        return logDir;
    }

    //上传未上传的日志
    public void uploadPendingLogs() {
        if (uploader == null) return;
        File[] logs = getLogDirectory().listFiles((dir, name) -> name.endsWith(".log"));
        if (logs == null || logs.length == 0) return;
        executor.execute(() -> uploadPendingLogs(logs));
    }

    private void uploadPendingLogs(File[] logs) {
        for (File log : logs) {
            uploader.upload(log, new UploadCallback() {
                @Override
                public void onSuccess(File file) {
                    if (file.exists()) {
                        boolean deleteResult = file.delete();
                        Log.w(TAG, "Delete result: " + deleteResult + " name: " + file.getName());
                    }
                }

                @Override
                public void onFailure(File file, Exception e) {
                    Log.w(TAG, "Upload failed: " + file.getName());
                }
            });
        }
    }
}