package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.profile.devlab.DevLabLogEntry;
import com.skyinit.pomodorotimer.ui.profile.devlab.DevLabLogLevel;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.SystemInfoUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 开发实验室数据源：应用/设备静态信息、存储/内存快照、logcat 采样。
 */
public class DevLabRepository {

    private static final String TAG = "DevLabRepository";
    private static final int MAX_LOGCAT_LINES = 80;

    private final Context appContext;
    private final AtomicLong logIdSeq = new AtomicLong(1L);

    public DevLabRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    public AppInfoSnapshot loadAppInfo() {
        try {
            PackageInfo packageInfo = appContext.getPackageManager()
                    .getPackageInfo(appContext.getPackageName(), 0);
            long versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }
            String versionName = packageInfo.versionName != null ? packageInfo.versionName : "";
            return new AppInfoSnapshot(
                    appContext.getString(R.string.app_name),
                    versionName,
                    versionCode,
                    false
            );
        } catch (PackageManager.NameNotFoundException e) {
            AppLog.e(TAG, "Error getting app info", e);
            return new AppInfoSnapshot(
                    appContext.getString(R.string.app_name),
                    appContext.getString(R.string.dev_lab_error_unavailable),
                    0L,
                    true
            );
        }
    }

    @NonNull
    public DeviceInfoSnapshot loadDeviceInfo() {
        return new DeviceInfoSnapshot(
                Build.DEVICE != null ? Build.DEVICE : Build.MODEL,
                Build.MODEL != null ? Build.MODEL : "",
                Build.BRAND != null ? Build.BRAND : "",
                Locale.getDefault().getDisplayLanguage(),
                Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "",
                formatFullApiLevel()
        );
    }

    @NonNull
    @WorkerThread
    public ResourceSnapshot loadStorage() {
        try {
            String appStorage = SystemInfoUtils.getAppStorageSize(appContext);
            String totalStorage = SystemInfoUtils.getTotalStorageSize(appContext);
            String usedStorage = SystemInfoUtils.getUsedStorageSize(appContext);
            String percentText = SystemInfoUtils.getStorageUsagePercentage(appContext);
            int percent = SystemInfoUtils.getStorageUsagePercent(appContext);
            return new ResourceSnapshot(appStorage, totalStorage, usedStorage, percentText, percent, false);
        } catch (Exception e) {
            AppLog.e(TAG, "Error loading storage info", e);
            String unavailable = appContext.getString(R.string.dev_lab_error_storage_info);
            return new ResourceSnapshot(unavailable, unavailable, unavailable, "—", 0, true);
        }
    }

    @NonNull
    @WorkerThread
    public ResourceSnapshot loadMemory() {
        try {
            String appMemory = SystemInfoUtils.getAppMemoryUsage(appContext);
            String totalMemory = SystemInfoUtils.getTotalMemorySize(appContext);
            String usedMemory = SystemInfoUtils.getUsedMemorySize(appContext);
            String percentText = SystemInfoUtils.getMemoryUsagePercentage(appContext);
            int percent = SystemInfoUtils.getMemoryUsagePercent(appContext);
            return new ResourceSnapshot(appMemory, totalMemory, usedMemory, percentText, percent, false);
        } catch (Exception e) {
            AppLog.e(TAG, "Error loading memory info", e);
            String unavailable = appContext.getString(R.string.dev_lab_error_memory_info);
            return new ResourceSnapshot(unavailable, unavailable, unavailable, "—", 0, true);
        }
    }

    @NonNull
    @WorkerThread
    public List<DevLabLogEntry> loadLogcatEntries() {
        List<DevLabLogEntry> entries = new ArrayList<>();
        String now = nowTimestamp();
        entries.add(new DevLabLogEntry(
                logIdSeq.getAndIncrement(),
                now,
                DevLabLogLevel.INFO,
                appContext.getString(R.string.dev_lab_log_category_app_start),
                appContext.getString(R.string.dev_lab_log_app_start)
        ));

        int logcatCount = 0;
        Process process = null;
        BufferedReader reader = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("logcat", "-d", "-v", "time", "-t", "200");
            process = processBuilder.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String packageName = appContext.getPackageName();
            while ((line = reader.readLine()) != null && logcatCount < MAX_LOGCAT_LINES) {
                if (line.contains("PomodoroTimer") || line.contains(packageName)) {
                    entries.add(new DevLabLogEntry(
                            logIdSeq.getAndIncrement(),
                            nowTimestamp(),
                            DevLabLogLevel.fromLogcatToken(line),
                            appContext.getString(R.string.dev_lab_log_system),
                            line
                    ));
                    logcatCount++;
                }
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Error reading logcat", e);
            String message = e.getMessage() != null ? e.getMessage() : "";
            entries.add(new DevLabLogEntry(
                    logIdSeq.getAndIncrement(),
                    nowTimestamp(),
                    DevLabLogLevel.ERROR,
                    appContext.getString(R.string.dev_lab_log_category_log_read),
                    appContext.getString(R.string.dev_lab_log_read_failed, message)
            ));
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        if (logcatCount == 0 && entries.size() == 1) {
            entries.add(new DevLabLogEntry(
                    logIdSeq.getAndIncrement(),
                    nowTimestamp(),
                    DevLabLogLevel.INFO,
                    appContext.getString(R.string.dev_lab_log_category_log_read),
                    appContext.getString(R.string.dev_lab_log_empty)
            ));
        }
        return entries;
    }

    @NonNull
    public DevLabLogEntry createLocalEntry(@NonNull DevLabLogLevel level,
                                           @NonNull String tag,
                                           @NonNull String message) {
        return new DevLabLogEntry(
                logIdSeq.getAndIncrement(),
                nowTimestamp(),
                level,
                tag,
                message
        );
    }

    @NonNull
    private static String nowTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    /** Android 16+ 使用 SDK_INT_FULL 区分 36.0 / 36.1；更低版本回退为主版本 + .0 */
    @NonNull
    private static String formatFullApiLevel() {
        if (Build.VERSION.SDK_INT >= 36) {
            int sdkFull = Build.VERSION.SDK_INT_FULL;
            int major = sdkFull / 100_000;
            int minor = sdkFull % 100_000;
            return String.format(Locale.US, "%d.%d", major, minor);
        }
        return String.format(Locale.US, "%d.0", Build.VERSION.SDK_INT);
    }

    public static final class AppInfoSnapshot {
        @NonNull public final String appName;
        @NonNull public final String versionName;
        public final long versionCode;
        public final boolean error;

        public AppInfoSnapshot(@NonNull String appName, @NonNull String versionName,
                               long versionCode, boolean error) {
            this.appName = appName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.error = error;
        }
    }

    public static final class DeviceInfoSnapshot {
        @NonNull public final String deviceName;
        @NonNull public final String deviceModel;
        @NonNull public final String deviceBrand;
        @NonNull public final String language;
        @NonNull public final String androidVersion;
        @NonNull public final String apiLevel;

        public DeviceInfoSnapshot(@NonNull String deviceName, @NonNull String deviceModel,
                                  @NonNull String deviceBrand, @NonNull String language,
                                  @NonNull String androidVersion, @NonNull String apiLevel) {
            this.deviceName = deviceName;
            this.deviceModel = deviceModel;
            this.deviceBrand = deviceBrand;
            this.language = language;
            this.androidVersion = androidVersion;
            this.apiLevel = apiLevel;
        }
    }

    public static final class ResourceSnapshot {
        @NonNull public final String app;
        @NonNull public final String total;
        @NonNull public final String used;
        @NonNull public final String percentText;
        public final int percent;
        public final boolean error;

        public ResourceSnapshot(@NonNull String app, @NonNull String total, @NonNull String used,
                                @NonNull String percentText, int percent, boolean error) {
            this.app = app;
            this.total = total;
            this.used = used;
            this.percentText = percentText;
            this.percent = percent;
            this.error = error;
        }
    }
}
