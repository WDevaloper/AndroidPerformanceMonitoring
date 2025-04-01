package com.github.crash.crash;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

public class DeviceInfoCollector {
    public String buildLogContent(Context context, Throwable ex, Thread thread) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== Crash Report ==========\n");

        // 时间信息
        appendSectionHeader(sb, "Crash Time");
        sb.append(formatTimestamp()).append("\n\n");

        // 设备信息
        appendSectionHeader(sb, "========== Device Information ==========");
        appendDeviceInfo(sb);
        appendSectionHeader(sb, "========== Device Information ==========");
        sb.append("\n");

        // 应用信息
        appendSectionHeader(sb, "========== Application Information ==========");
        appendAppInfo(sb, context);
        appendSectionHeader(sb, "========== Application Information ==========");
        sb.append("\n");

        // 显示信息
        appendSectionHeader(sb, "========== Display Information ==========");
        appendDisplayInfo(sb, context);
        appendSectionHeader(sb, "========== Display Information ==========");
        sb.append("\n");

        // 运行时信息
        appendSectionHeader(sb, "========== Runtime Information ==========");
        appendRuntimeInfo(sb);
        appendSectionHeader(sb, "========== Runtime Information ==========");
        sb.append("\n");

        // CPU 信息
        appendSectionHeader(sb, "========== CPU Information ==========");
        appendCPUInfo(sb);
        appendSectionHeader(sb, "========== CPU Information ==========");
        sb.append("\n");

        // 内存信息
        appendSectionHeader(sb, "========== Memory Information ==========");
        appendMemoryInfo(sb);
        appendSectionHeader(sb, "========== Memory Information ==========");
        sb.append("\n");

        // 存储信息
        appendSectionHeader(sb, "========== Storage Information ==========");
        appendStorageInfo(sb);
        appendSectionHeader(sb, "========== Storage Information ==========");
        sb.append("\n");

        // 异常堆栈
        appendSectionHeader(sb, "========== Exception Stack Trace ==========");
        appendStackTrace(sb, ex);
        appendSectionHeader(sb, "========== Exception Stack Trace ==========");

        sb.append("\n========== End Report ==========\n");
        return sb.toString();
    }

    private static void appendSectionHeader(StringBuilder sb, String title) {
        sb.append("[").append(title).append("]\n");
    }

    private static String formatTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.getDefault())
                .format(new Date());
    }

    private static void appendDeviceInfo(StringBuilder sb) {
        sb.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        sb.append("Model: ").append(Build.MODEL).append("\n");
        sb.append("Product: ").append(Build.PRODUCT).append("\n");
        sb.append("Device: ").append(Build.DEVICE).append("\n");
        sb.append("Board: ").append(Build.BOARD).append("\n");
        sb.append("Hardware: ").append(Build.HARDWARE).append("\n");
        sb.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("API Level: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("Build ID: ").append(Build.DISPLAY).append("\n");
        sb.append("Fingerprint: ").append(Build.FINGERPRINT).append("\n");
        sb.append("Bootloader: ").append(Build.BOOTLOADER).append("\n");
    }

    private void appendAppInfo(StringBuilder sb, Context context) {
        try {
            PackageInfo pkgInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);

            sb.append("Package Name: ").append(context.getPackageName()).append("\n");
            sb.append("Version Name: ").append(pkgInfo.versionName).append("\n");
            sb.append("Version Code: ").append(pkgInfo.versionCode).append("\n");
            sb.append("First Install Time: ").append(formatDate(pkgInfo.firstInstallTime)).append("\n");
            sb.append("Last Update Time: ").append(formatDate(pkgInfo.lastUpdateTime)).append("\n");

        } catch (PackageManager.NameNotFoundException e) {
            Log.w("CrashLogger", "Failed to get package info", e);
            sb.append("Package Info Unavailable\n");
        }
    }

    private void appendDisplayInfo(StringBuilder sb, Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        sb.append("Resolution: ").append(metrics.widthPixels).append("x")
                .append(metrics.heightPixels).append("\n");
        sb.append("Density: ").append(metrics.density).append("\n");
        sb.append("Density DPI: ").append(metrics.densityDpi).append("dpi\n");
        sb.append("Scaled Density: ").append(metrics.scaledDensity).append("\n");
    }

    private void appendRuntimeInfo(StringBuilder sb) {
        Runtime runtime = Runtime.getRuntime();
        sb.append("Available Processors: ").append(runtime.availableProcessors()).append("\n");
        sb.append("Total Memory: ").append(formatFileSize(runtime.totalMemory())).append("\n");
        sb.append("Free Memory: ").append(formatFileSize(runtime.freeMemory())).append("\n");
        sb.append("Max Memory: ").append(formatFileSize(runtime.maxMemory())).append("\n");
    }

    private void appendCPUInfo(StringBuilder sb) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            br.lines().forEach(line -> {
                if (line != null) sb.append(line).append("\n");
            });
            br.close();
        } catch (IOException e) {
            Log.w("CrashLogger", "Failed to get CPU info", e);
            sb.append("CPU Info Unavailable\n");
        }

        sb.append("Uptime: ").append(formatTime(SystemClock.uptimeMillis())).append("\n");
    }

    private void appendMemoryInfo(StringBuilder sb) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    sb.append("Total Memory: ").append(formatFileSize(parseMemInfo(line))).append("\n");
                } else if (line.startsWith("MemFree:")) {
                    sb.append("Free Memory: ").append(formatFileSize(parseMemInfo(line))).append("\n");
                } else if (line.startsWith("MemAvailable:")) {
                    sb.append("Available Memory: ").append(formatFileSize(parseMemInfo(line))).append("\n");
                } else if (line.startsWith("Buffers:")) {
                    sb.append("Buffers: ").append(formatFileSize(parseMemInfo(line))).append("\n");
                } else if (line.startsWith("Cached:")) {
                    sb.append("Cached: ").append(formatFileSize(parseMemInfo(line))).append("\n");
                }
            }
            br.close();
        } catch (IOException e) {
            Log.w("CrashLogger", "Failed to get memory info", e);
            sb.append("Memory Info Unavailable\n");
        }
    }

    private long parseMemInfo(String line) {
        String[] parts = line.split("\\s+");
        return Long.parseLong(parts[1]) * 1024;
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        hours %= 24;
        minutes %= 60;
        seconds %= 60;
        return String.format(Locale.US, "%d days, %02d:%02d:%02d", days, hours, minutes, seconds);
    }

    private void appendStorageInfo(StringBuilder sb) {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());

            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();

            sb.append("Internal Storage:\n");
            sb.append("  Total: ").append(formatFileSize(totalBlocks * blockSize)).append("\n");
            sb.append("  Available: ").append(formatFileSize(availableBlocks * blockSize)).append("\n");

        } catch (IllegalArgumentException e) {
            Log.w("CrashLogger", "Failed to get storage info", e);
            sb.append("Storage Info Unavailable\n");
        }
    }

    private void appendStackTrace(StringBuilder sb, Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        sb.append(sw);
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(timestamp));
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format(Locale.US, "%.1f %s",
                bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}