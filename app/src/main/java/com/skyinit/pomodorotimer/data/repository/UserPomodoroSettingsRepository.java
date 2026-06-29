package com.skyinit.pomodorotimer.data.repository;

import androidx.annotation.NonNull;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.UserPomodoroSettingsDao;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.util.AppExecutors;

/**
 * 用户级番茄钟工作流设置的 Room 数据源。
 * <p>
 * 主线程通过内存缓存读取，避免 Service/UI 线程直接访问 Room。
 */
public class UserPomodoroSettingsRepository {

    private final UserPomodoroSettingsDao dao;
    private final AccountManager accountManager;
    private final AppExecutors executors = AppExecutors.getInstance();

    private volatile UserPomodoroSettings memoryCache;

    public UserPomodoroSettingsRepository(AppDatabase database, AccountManager accountManager) {
        this.dao = database.userPomodoroSettingsDao();
        this.accountManager = accountManager;
    }

    /** 后台预加载当前用户的设置到内存缓存（需已 {@link AccountManager#ensureDefaultProfile()}）。 */
    public void warmCache() {
        executors.diskIo(() -> {
            if (accountManager.getCurrentUser() == null) {
                return;
            }
            loadIntoCache();
        });
    }

    /**
     * 主线程安全：读取当前用户设置。
     * 缓存未就绪时返回默认值并触发后台加载。
     */
    @NonNull
    public UserPomodoroSettings getSettings() {
        String userId = accountManager.requireActiveUserId();
        UserPomodoroSettings cached = memoryCache;
        if (cached != null && userId.equals(cached.userId)) {
            return copySettings(cached);
        }
        warmCache();
        UserPomodoroSettings defaults = new UserPomodoroSettings();
        defaults.userId = userId;
        return defaults;
    }

    /** 主线程安全：更新内存缓存并异步写入 Room。 */
    public void saveSettings(UserPomodoroSettings settings) {
        UserPomodoroSettings snapshot = copySettings(settings);
        memoryCache = snapshot;
        executors.diskIo(() -> dao.upsert(snapshot));
    }

    public void invalidateCache() {
        memoryCache = null;
    }

    /** 仅后台线程调用。 */
    @NonNull
    public UserPomodoroSettings getSettingsSync() {
        return loadIntoCache();
    }

    public void updateSettings(UserPomodoroSettings settings) {
        saveSettings(settings);
    }

    /** 仅后台线程调用。 */
    public void updateSettingsSync(UserPomodoroSettings settings) {
        UserPomodoroSettings snapshot = copySettings(settings);
        memoryCache = snapshot;
        dao.upsert(snapshot);
    }

    public void ensureDefaultsForUser(@NonNull String userId) {
        executors.diskIo(() -> {
            if (dao.getByUserId(userId) == null) {
                dao.upsert(new UserPomodoroSettings(userId));
            }
            User current = accountManager.getCurrentUser();
            if (current != null && userId.equals(current.userId)) {
                loadIntoCache();
            }
        });
    }

    @NonNull
    private UserPomodoroSettings loadIntoCache() {
        User current = accountManager.getCurrentUser();
        if (current == null) {
            throw new IllegalStateException("No active profile; call ensureDefaultProfile() first");
        }
        String userId = current.userId;
        UserPomodoroSettings settings = dao.getByUserId(userId);
        if (settings == null) {
            settings = new UserPomodoroSettings(userId);
            dao.upsert(settings);
        }
        memoryCache = copySettings(settings);
        return memoryCache;
    }

    @NonNull
    public static UserPomodoroSettings copySettings(UserPomodoroSettings src) {
        UserPomodoroSettings copy = new UserPomodoroSettings();
        copy.userId = src.userId != null ? src.userId : "";
        copy.defaultStudyTimeMs = src.defaultStudyTimeMs;
        copy.defaultBreakTimeMs = src.defaultBreakTimeMs;
        copy.maxPauseCount = src.maxPauseCount;
        copy.dndDuringFocusEnabled = src.dndDuringFocusEnabled;
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
