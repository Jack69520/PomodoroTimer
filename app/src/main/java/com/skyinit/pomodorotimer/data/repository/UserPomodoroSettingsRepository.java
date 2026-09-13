package com.skyinit.pomodorotimer.data.repository;

import androidx.annotation.NonNull;

import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

/**
 * 设备级番茄钟工作流设置（SharedPreferences）。
 * 对外仍暴露 {@link UserPomodoroSettings} 快照以兼容既有 ViewModel / Service。
 */
public class UserPomodoroSettingsRepository {

    private static final String TAG = "UserPomodoroSettingsRepository";

    public interface SettingsMutator {
        void mutate(@NonNull UserPomodoroSettings settings);
    }

    private final SettingsManager settingsManager;
    private final AppExecutors executors = AppExecutors.getInstance();

    private final Object cacheLock = new Object();
    private volatile UserPomodoroSettings memoryCache;

    public UserPomodoroSettingsRepository(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void warmCache() {
        executors.diskIo(this::warmCacheOnDisk,
                throwable -> AppLog.e(TAG, "Failed to warm pomodoro settings cache", throwable));
    }

    public void warmCacheOnDisk() {
        loadIntoCache();
    }

    @NonNull
    public UserPomodoroSettings getSettings() {
        UserPomodoroSettings cached = peekCache();
        if (cached != null) {
            return copySettings(cached);
        }
        warmCache();
        return settingsManager.getPomodoroSettings();
    }

    public void saveSettings(UserPomodoroSettings settings) {
        UserPomodoroSettings snapshot = copySettings(settings);
        publishCache(snapshot);
        persistSnapshot(snapshot);
    }

    public void invalidateCache() {
        synchronized (cacheLock) {
            memoryCache = null;
        }
    }

    @NonNull
    public UserPomodoroSettings getSettingsSync() {
        return copySettings(loadIntoCache());
    }

    public void updateSettings(UserPomodoroSettings settings) {
        saveSettings(settings);
    }

    public void updateSettingsSync(UserPomodoroSettings settings) {
        UserPomodoroSettings snapshot = copySettings(settings);
        publishCache(snapshot);
        settingsManager.savePomodoroSettings(snapshot);
    }

    public void applyUpdate(@NonNull SettingsMutator mutator) {
        if (executors.isDiskIoThread()) {
            applyUpdateOnDisk(mutator);
            return;
        }

        synchronized (cacheLock) {
            UserPomodoroSettings cached = memoryCache;
            if (cached != null) {
                UserPomodoroSettings working = copySettings(cached);
                mutator.mutate(working);
                memoryCache = copySettings(working);
            }
        }

        executors.diskIo(() -> applyUpdateOnDisk(mutator),
                throwable -> AppLog.e(TAG, "Failed to apply pomodoro settings update", throwable));
    }

    public void clearCycleCountOnSessionSwitch() {
        settingsManager.clearPomodoroCycleCount();
        synchronized (cacheLock) {
            if (memoryCache != null) {
                memoryCache.pomodoroCycleCount = 0;
            }
        }
    }

    private void applyUpdateOnDisk(@NonNull SettingsMutator mutator) {
        UserPomodoroSettings fromStore = settingsManager.getPomodoroSettings();
        UserPomodoroSettings working = copySettings(fromStore);
        mutator.mutate(working);
        updateSettingsSync(working);
    }

    @NonNull
    private UserPomodoroSettings peekCache() {
        return memoryCache;
    }

    private void publishCache(@NonNull UserPomodoroSettings snapshot) {
        synchronized (cacheLock) {
            memoryCache = snapshot;
        }
    }

    private void persistSnapshot(@NonNull UserPomodoroSettings snapshot) {
        if (executors.isDiskIoThread()) {
            try {
                settingsManager.savePomodoroSettings(snapshot);
            } catch (Throwable throwable) {
                AppLog.e(TAG, "Failed to save pomodoro settings", throwable);
            }
            return;
        }
        executors.diskIo(() -> settingsManager.savePomodoroSettings(snapshot),
                throwable -> AppLog.e(TAG, "Failed to save pomodoro settings", throwable));
    }

    @NonNull
    private UserPomodoroSettings loadIntoCache() {
        UserPomodoroSettings settings = settingsManager.getPomodoroSettings();
        UserPomodoroSettings snapshot = copySettings(settings);
        publishCache(snapshot);
        return snapshot;
    }

    @NonNull
    public static UserPomodoroSettings copySettings(UserPomodoroSettings src) {
        UserPomodoroSettings copy = new UserPomodoroSettings();
        copy.userId = src.userId != null ? src.userId : "";
        copy.defaultStudyTimeMs = src.defaultStudyTimeMs;
        copy.defaultBreakTimeMs = src.defaultBreakTimeMs;
        copy.maxPauseCount = src.maxPauseCount;
        copy.dndDuringFocusEnabled = src.dndDuringFocusEnabled;
        copy.autoBlockDuringPomodoro = src.autoBlockDuringPomodoro;
        copy.lockScreenFullscreenEnabled = src.lockScreenFullscreenEnabled;
        copy.autoStartAfterBreak = src.autoStartAfterBreak;
        copy.longBreakEnabled = src.longBreakEnabled;
        copy.pomodorosBeforeLongBreak = src.pomodorosBeforeLongBreak;
        copy.longBreakDurationMs = src.longBreakDurationMs;
        copy.pomodoroCycleCount = src.pomodoroCycleCount;
        return copy;
    }

    public static int clampPomodorosBeforeLongBreak(int value) {
        return Math.max(UserPomodoroSettings.MIN_POMODOROS_BEFORE_LONG_BREAK, value);
    }

    public static long clampLongBreakDurationMs(long millis) {
        return Math.max(
                UserPomodoroSettings.MIN_LONG_BREAK_MS,
                Math.min(UserPomodoroSettings.MAX_LONG_BREAK_MS, millis)
        );
    }
}
