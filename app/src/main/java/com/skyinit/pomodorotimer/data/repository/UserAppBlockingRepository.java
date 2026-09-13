package com.skyinit.pomodorotimer.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.UserAppBlockingDao;
import com.skyinit.pomodorotimer.data.entity.UserAppBlocking;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

/**
 * 按当前注册用户隔离的应用屏蔽开关；主线程通过内存缓存读取。
 */
public final class UserAppBlockingRepository {

    private static final String TAG = "UserAppBlockingRepository";

    private final UserAppBlockingDao dao;
    private final AccountManager accountManager;
    private final AppExecutors executors = AppExecutors.getInstance();

    private final Object cacheLock = new Object();
    @Nullable
    private String cachedUserId;
    private boolean cachedEnabled;

    public UserAppBlockingRepository(AppDatabase database, AccountManager accountManager) {
        this.dao = database.userAppBlockingDao();
        this.accountManager = accountManager;
    }

    public boolean isEnabledForCurrentUser() {
        String userId = accountManager.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        synchronized (cacheLock) {
            if (userId.equals(cachedUserId)) {
                return cachedEnabled;
            }
        }
        if (executors.isDiskIoThread()) {
            return loadAndCache(userId);
        }
        return false;
    }

    @WorkerThread
    public boolean isEnabledSync(@NonNull String userId) {
        return loadAndCache(userId);
    }

    public void setEnabledForCurrentUser(boolean enabled) {
        String userId = accountManager.getCurrentUserId();
        if (userId == null) {
            return;
        }
        setEnabled(userId, enabled);
    }

    public void setEnabled(@NonNull String userId, boolean enabled) {
        publishCache(userId, enabled);
        if (executors.isDiskIoThread()) {
            dao.upsert(new UserAppBlocking(userId, enabled));
            return;
        }
        executors.diskIo(() -> dao.upsert(new UserAppBlocking(userId, enabled)),
                t -> AppLog.e(TAG, "Failed to save blocking enabled", t));
    }

    /** 登录/恢复会话后在 diskIo 调用，预热缓存。 */
    @WorkerThread
    public void warmForUser(@Nullable String userId) {
        if (userId == null || userId.isEmpty()) {
            clearCache();
            return;
        }
        loadAndCache(userId);
    }

    public void clearCache() {
        synchronized (cacheLock) {
            cachedUserId = null;
            cachedEnabled = false;
        }
    }

    @WorkerThread
    public void ensureRow(@NonNull String userId) {
        if (dao.getByUserId(userId) == null) {
            dao.upsert(new UserAppBlocking(userId, false));
        }
        loadAndCache(userId);
    }

    private boolean loadAndCache(@NonNull String userId) {
        UserAppBlocking row = dao.getByUserId(userId);
        boolean enabled = row != null && row.enabled;
        publishCache(userId, enabled);
        return enabled;
    }

    private void publishCache(@NonNull String userId, boolean enabled) {
        synchronized (cacheLock) {
            cachedUserId = userId;
            cachedEnabled = enabled;
        }
    }
}
