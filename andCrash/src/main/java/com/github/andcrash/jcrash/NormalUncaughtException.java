package com.github.andcrash.jcrash;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class NormalUncaughtException implements IUncaughtExceptionHandler {
    private final DeviceInfoCollector deviceInfoCollector = new DeviceInfoCollector();

    @Override
    public void uncaughtException(Context context, String logDir, Thread thread, Throwable ex) throws IOException {
        File logFile = new File(logDir, "crash_" + System.currentTimeMillis() + ".log");
        byte[] logContent =
                deviceInfoCollector.buildLogContent(context, ex, thread).getBytes();
        long dataLength = logContent.length;//可以固定map大小，也可以通过数据计算
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "rw");
             FileChannel channel = raf.getChannel()) {
            MappedByteBuffer buffer =
                    channel.map(FileChannel.MapMode.READ_WRITE, 0, dataLength);
            buffer.put(logContent);
            buffer.force();
            mappedByteBufferCleaner(buffer);  // 显式清理 MappedByteBuffer 资源
            Log.d(AndCrash.TAG, "Crash log saved: " + logFile.getName());
        } catch (IOException e) {
            Log.e(AndCrash.TAG, "Failed to save crash log", e);
            throw e;
        }
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
            Log.w(AndCrash.TAG, "Failed to clean MappedByteBuffer resources", e);
        }
    }


    @Override
    public boolean handleCrashAfter(Context context) {
        return false;
    }

    @Override
    public boolean isHandledable(Throwable ex) {
        return true;
    }
}
