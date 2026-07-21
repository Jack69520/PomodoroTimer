package com.skyinit.pomodorotimer.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.UserPomodoroSettingsDao;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

/**
 * 用户级番茄钟工作流设置的 Room 数据源。
 * <p>
 * 主线程通过内存缓存读取，避免 Service/UI 线程直接访问 Room。
 * 写路径保证：先更新内存缓存；若已在 diskIo 线程则同步 upsert，否则异步落盘，
 * 避免“缓存已新、再 getSettingsSync 读到旧 DB 并把缓存打回”的竞态。
 */
public class UserPomodoroSettingsRepository {

    private static final String TAG = "UserPomodoroSettingsRepository";

    public interface SettingsMutator {
        void mutate(@NonNull UserPomodoroSettings settings);
    }

    private final UserPomodoroSettingsDao dao;
    private final AccountManager accountManager;
    private final AppExecutors executors = AppExecutors.getInstance();

    private final Object cacheLock = new Object();
    private volatile UserPomodoroSettings memoryCache;

    public UserPomodoroSettingsRepository(AppDatabase database, AccountManager accountManager) {
        this.dao = database.userPomodoroSettingsDao();
        this.accountManager = accountManager;
    }

    /** 后台预加载当前用户的设置到内存缓存（需已完成应用初始化）。 */
    public void warmCache() {
        executors.diskIo(() -> warmCacheOnDisk(),
                throwable -> AppLog.e(TAG, "Failed to warm pomodoro settings cache", throwable));
    }

    /**
     * 在 diskIo 线程内同步预热缓存。
     * 应用引导流程已在 diskIo 上运行时必须调用本方法，避免单线程池重入死锁。
     */
    public void warmCacheOnDisk() {
        User current = accountManager.getCurrentUser();
        if (current == null) {
            AppLog.w(TAG, "Skip warmCache because active profile is not ready");
            return;
        }
        loadIntoCache();
    }

    /**
     * 主线程安全：读取当前用户设置。
     * 缓存未就绪时返回默认值并触发后台加载（返回值不得用于整行写回）。
     */
    @NonNull
    public UserPomodoroSettings getSettings() {
        String userId = accountManager.requireActiveUserId();
        UserPomodoroSettings cached = peekCache(userId);
        if (cached != null) {
            return copySettings(cached);
        }
        warmCache();
        UserPomodoroSettings defaults = new UserPomodoroSettings();
        defaults.userId = userId;
        return defaults;
    }

    /**
     * 主线程安全：更新内存缓存并写入 Room。
     * 已在 diskIo 上下文时同步 upsert，避免同队列上“先排队写、再同步读 DB”乱序。
     */
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

    /** 仅后台线程调用。 */
    @NonNull
    public UserPomodoroSettings getSettingsSync() {
        return copySettings(loadIntoCache());
    }

    public void updateSettings(UserPomodoroSettings settings) {
        saveSettings(settings);
    }

    /** 仅后台线程调用：更新缓存并同步 upsert。 */
    public void updateSettingsSync(UserPomodoroSettings settings) {
        UserPomodoroSettings snapshot = copySettings(settings);
        publishCache(snapshot);
        dao.upsert(snapshot);
    }

    /**
     * 基于最新设置做字段级变更并持久化。
     * <ul>
     *   <li>已在 diskIo：同步 load → mutate → upsert。</li>
     *   <li>其它线程：缓存命中时先乐观更新内存（供 UI 立即读到）；再投递 diskIo 对 DB 做
     *       串行 RMW，避免多字段并发写互相覆盖，也避免用默认值整行写回。</li>
     * </ul>
     */
    public void applyUpdate(@NonNull SettingsMutator mutator) {
        String userId = accountManager.requireActiveUserId();
        if (executors.isDiskIoThread()) {
            applyUpdateOnDisk(userId, mutator);
            return;
        }

        synchronized (cacheLock) {
            UserPomodoroSettings cached = memoryCache;
            if (cached != null && userId.equals(cached.userId)) {
                UserPomodoroSettings working = copySettings(cached);
                mutator.mutate(working);
                working.userId = userId;
                memoryCache = copySettings(working);
            }
        }

        executors.diskIo(() -> applyUpdateOnDisk(userId, mutator),
                throwable -> AppLog.e(TAG, "Failed to apply pomodoro settings update", throwable));
    }

    private void applyUpdateOnDisk(@NonNull String userId, @NonNull SettingsMutator mutator) {
        UserPomodoroSettings fromDb = dao.getByUserId(userId);
        if (fromDb == null) {
            fromDb = new UserPomodoroSettings(userId);
        }
        UserPomodoroSettings working = copySettings(fromDb);
        mutator.mutate(working);
        working.userId = userId;
        updateSettingsSync(working);
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
        }, throwable -> AppLog.e(TAG,
                "Failed to ensure default pomodoro settings for user " + userId, throwable));
    }

    @Nullable
    private UserPomodoroSettings peekCache(@NonNull String userId) {
        UserPomodoroSettings cached = memoryCache;
        if (cached != null && userId.equals(cached.userId)) {
            return cached;
        }
        return null;
    }

    private void publishCache(@NonNull UserPomodoroSettings snapshot) {
        synchronized (cacheLock) {
            memoryCache = snapshot;
        }
    }

    private void persistSnapshot(@NonNull UserPomodoroSettings snapshot) {
        if (executors.isDiskIoThread()) {
            try {
                dao.upsert(snapshot);
            } catch (Throwable throwable) {
                AppLog.e(TAG, "Failed to save pomodoro settings", throwable);
            }
            return;
        }
        executors.diskIo(() -> dao.upsert(snapshot),
                throwable -> AppLog.e(TAG, "Failed to save pomodoro settings", throwable));
    }

    @NonNull
    private UserPomodoroSettings loadIntoCache() {
        User current = accountManager.getCurrentUser();
        if (current == null) {
            throw new IllegalStateException("No active profile; wait for app initialization");
        }
        String userId = current.userId;
        UserPomodoroSettings settings = dao.getByUserId(userId);
        if (settings == null) {
            settings = new UserPomodoroSettings(userId);
            dao.upsert(settings);
        }
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
