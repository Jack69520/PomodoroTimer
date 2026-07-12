package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.R;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.Debug;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;

public class SystemInfoUtils {
    private static final String TAG = "SystemInfoUtils";

    private static Context appContext;

    private static Context ctx(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            return appContext;
        }
        return appContext;
    }

    private static String unavailable(Context context) {
        Context c = ctx(context);
        return c != null ? c.getString(R.string.dev_lab_error_unavailable) : "";
    }

    private static String errorString(Context context, int resId) {
        Context c = ctx(context);
        return c != null ? c.getString(resId) : unavailable(context);
    }

    /**
     * 获取应用占用的存储空间
     */
    public static String getAppStorageSize(Context context) {
        try {
            File appDir = context.getFilesDir().getParentFile();
            if (appDir == null || !appDir.exists()) {
                return errorString(context, R.string.dev_lab_error_directory_missing);
            }
            long appSize = getFolderSize(appDir);
            return formatFileSize(appSize);
        } catch (SecurityException e) {
            Log.e(TAG, "Security error getting app storage size", e);
            return errorString(context, R.string.dev_lab_error_permission_denied);
        } catch (Exception e) {
            Log.e(TAG, "Error getting app storage size", e);
            return unavailable(context);
        }
    }

    /**
     * 获取应用占用的运行内存
     */
    public static String getAppMemoryUsage(Context context) {
        try {
            Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memoryInfo);

            long pssMemory = memoryInfo.getTotalPss() * 1024;
            return formatFileSize(pssMemory);
        } catch (Exception e) {
            Log.e(TAG, "Error getting app memory usage", e);
            return unavailable(context);
        }
    }

    /**
     * 获取设备总存储空间
     */
    public static String getTotalStorageSize() {
        return getTotalStorageSize(null);
    }

    public static String getTotalStorageSize(Context context) {
        try {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return errorString(context, R.string.dev_lab_error_storage_unmounted);
            }
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long totalSize = stat.getTotalBytes();
            if (totalSize <= 0) {
                return errorString(context, R.string.dev_lab_error_data_abnormal);
            }
            return formatFileSize(totalSize);
        } catch (SecurityException e) {
            Log.e(TAG, "Security error getting total storage size", e);
            return errorString(context, R.string.dev_lab_error_permission_denied);
        } catch (Exception e) {
            Log.e(TAG, "Error getting total storage size", e);
            return unavailable(context);
        }
    }

    /**
     * 获取设备已用存储空间
     */
    public static String getUsedStorageSize() {
        return getUsedStorageSize(null);
    }

    public static String getUsedStorageSize(Context context) {
        try {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return errorString(context, R.string.dev_lab_error_storage_unmounted);
            }
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long totalSize = stat.getTotalBytes();
            long availableSize = stat.getAvailableBytes();
            if (totalSize <= 0 || availableSize < 0) {
                return errorString(context, R.string.dev_lab_error_data_abnormal);
            }
            long usedSize = totalSize - availableSize;
            return formatFileSize(usedSize);
        } catch (SecurityException e) {
            Log.e(TAG, "Security error getting used storage size", e);
            return errorString(context, R.string.dev_lab_error_permission_denied);
        } catch (Exception e) {
            Log.e(TAG, "Error getting used storage size", e);
            return unavailable(context);
        }
    }

    /**
     * 获取存储空间使用百分比
     */
    public static String getStorageUsagePercentage() {
        return getStorageUsagePercentage(null);
    }

    public static String getStorageUsagePercentage(Context context) {
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long totalSize = stat.getTotalBytes();
            long availableSize = stat.getAvailableBytes();
            long usedSize = totalSize - availableSize;

            double percentage = (double) usedSize / totalSize * 100;
            return String.format("%.1f%%", percentage);
        } catch (Exception e) {
            Log.e(TAG, "Error getting storage usage percentage", e);
            return unavailable(context);
        }
    }

    /**
     * 获取设备总运行内存
     */
    public static String getTotalMemorySize() {
        return getTotalMemorySize(null);
    }

    public static String getTotalMemorySize(Context context) {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
            String load = reader.readLine();
            reader.close();

            String[] toks = load.split("\\s+");
            long totalMemory = Long.parseLong(toks[1]) * 1024;
            return formatFileSize(totalMemory);
        } catch (Exception e) {
            Log.e(TAG, "Error getting total memory size", e);
            return unavailable(context);
        }
    }

    /**
     * 获取当前已占用的运行内存
     */
    public static String getUsedMemorySize(Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            long totalMemory = getTotalMemoryBytes();
            long usedMemory = totalMemory - memoryInfo.availMem;
            return formatFileSize(usedMemory);
        } catch (Exception e) {
            Log.e(TAG, "Error getting used memory size", e);
            return unavailable(context);
        }
    }

    /**
     * 获取内存使用百分比
     */
    public static String getMemoryUsagePercentage(Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            long totalMemory = getTotalMemoryBytes();
            long usedMemory = totalMemory - memoryInfo.availMem;

            double percentage = (double) usedMemory / totalMemory * 100;
            return String.format("%.1f%%", percentage);
        } catch (Exception e) {
            Log.e(TAG, "Error getting memory usage percentage", e);
            return unavailable(context);
        }
    }

    /**
     * 获取总内存字节数
     */
    private static long getTotalMemoryBytes() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
            String load = reader.readLine();
            reader.close();

            String[] toks = load.split("\\s+");
            return Long.parseLong(toks[1]) * 1024;
        } catch (Exception e) {
            Log.e(TAG, "Error getting total memory bytes", e);
            return 0;
        }
    }

    /**
     * 计算文件夹大小
     */
    private static long getFolderSize(File folder) {
        long size = 0;
        try {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getFolderSize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating folder size", e);
        }
        return size;
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * 获取系统信息摘要
     */
    public static String getSystemInfoSummary(Context context) {
        ctx(context);
        StringBuilder sb = new StringBuilder();
        sb.append(context.getString(R.string.dev_lab_label_storage_section));
        sb.append(context.getString(R.string.dev_lab_label_app_storage, getAppStorageSize(context)));
        sb.append(context.getString(R.string.dev_lab_label_total_storage, getTotalStorageSize(context)));
        sb.append(context.getString(R.string.dev_lab_label_used_storage,
                getUsedStorageSize(context), getStorageUsagePercentage(context)));

        sb.append(context.getString(R.string.dev_lab_label_memory_section));
        sb.append(context.getString(R.string.dev_lab_label_app_memory, getAppMemoryUsage(context)));
        sb.append(context.getString(R.string.dev_lab_label_total_memory, getTotalMemorySize(context)));
        sb.append(context.getString(R.string.dev_lab_label_used_memory,
                getUsedMemorySize(context), getMemoryUsagePercentage(context)));

        return sb.toString();
    }
}
