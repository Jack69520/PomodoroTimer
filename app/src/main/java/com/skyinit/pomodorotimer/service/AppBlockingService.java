package com.skyinit.pomodorotimer.service;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.BlockedAppDao;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.AppScanner;
import com.skyinit.pomodorotimer.util.FocusBlockNavigation;
import com.skyinit.pomodorotimer.util.FocusBlockOverlayManager;
import com.skyinit.pomodorotimer.util.PermissionUtils;
import com.skyinit.pomodorotimer.util.WhitelistManager;
import com.skyinit.pomodorotimer.R;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppBlockingService extends Service {
    private static final String TAG = "AppBlockingService";
    private static final String CHANNEL_ID = "app_blocking_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long MAX_SERVICE_DURATION = 5 * 60 * 60 * 1000L;
    private static final long CHECK_INTERVAL_MS = 2500L;
    private static final long CACHE_REFRESH_INTERVAL_MS = 60_000L;
    private static final long MIN_BLOCK_INTERVAL_MS = 2500L;

    private Handler mainHandler;
    private Handler timeoutHandler;
    private HandlerThread monitorThread;
    private Handler monitorHandler;
    private Runnable monitorRunnable;

    private BlockedAppDao blockedAppDao;
    private FocusBlockOverlayManager overlayManager;

    private final Set<String> enabledBlockPackages = ConcurrentHashMap.newKeySet();
    private final Set<String> whitelistedPackages = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean cacheLoaded = new AtomicBoolean(false);
    private long lastCacheRefreshMs;

    private boolean isBlockingEnabled;
    /** 本次屏蔽服务固定绑定的账户，避免账户切换后继续使用其他账户规则。 */
    private String blockingUserId;
    private long serviceStartTime;
    private int blockingCount;
    private String lastBlockedApp;
    private long lastBlockTime;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        timeoutHandler = new Handler(Looper.getMainLooper());
        overlayManager = new FocusBlockOverlayManager(this);
        blockedAppDao = AppDatabase.getDatabase(this).blockedAppDao();
        serviceStartTime = System.currentTimeMillis();
        createNotificationChannel();
        // 不在 onCreate 中进入前台：仅在学习计时且用户开启屏蔽时由 startBlocking() 展示通知
    }

    /** API 34+ 需显式传入 specialUse 类型，与 Manifest 声明一致。 */
    private void startBlockingForeground() {
        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                );
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("start_blocking".equals(action)) {
                startBlocking();
                if (!isBlockingEnabled) {
                    stopSelf();
                }
                return isBlockingEnabled ? START_STICKY : START_NOT_STICKY;
            } else if ("stop_blocking".equals(action)) {
                stopBlockingAndService();
                return START_NOT_STICKY;
            } else if ("check_timeout".equals(action)) {
                checkServiceTimeout();
            }
        }
        // 非屏蔽状态（含仅做超时检查的启动）不应常驻，避免误显示「专注模式运行中」
        if (!isBlockingEnabled) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopMonitoringInternal();
        overlayManager.hide();
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBlocking() {
        if (!PermissionUtils.hasUsageStatsPermission(this)) {
            AppLog.w(TAG, "Usage stats permission not granted");
            isBlockingEnabled = false;
            return;
        }

        String activeUserId = AccountManager.getInstance(this).requireActiveUserId();
        if (activeUserId == null || activeUserId.isEmpty()) {
            AppLog.w(TAG, "Cannot start blocking without active user");
            isBlockingEnabled = false;
            return;
        }

        if (isBlockingEnabled) {
            stopMonitoringInternal();
        }

        blockingUserId = activeUserId;
        isBlockingEnabled = true;
        serviceStartTime = System.currentTimeMillis();
        startBlockingForeground();
        scheduleAutoStop();
        blockingCount = 0;
        lastBlockedApp = null;
        lastBlockTime = 0L;
        cacheLoaded.set(false);
        refreshBlockCacheAsync(true);

        if (monitorThread == null) {
            monitorThread = new HandlerThread("AppBlockingMonitor");
            monitorThread.start();
            monitorHandler = new Handler(monitorThread.getLooper());
        }

        monitorRunnable = () -> {
            try {
                if (isBlockingEnabled) {
                    checkAndBlockApps();
                }
            } catch (Throwable t) {
                AppLog.e(TAG, "Monitor error", t);
            } finally {
                if (isBlockingEnabled && monitorHandler != null && monitorRunnable != null) {
                    monitorHandler.postDelayed(monitorRunnable, CHECK_INTERVAL_MS);
                }
            }
        };
        monitorHandler.post(monitorRunnable);
        AppLog.d(TAG, "App blocking started");
    }

    private void stopMonitoringInternal() {
        isBlockingEnabled = false;
        blockingUserId = null;
        if (monitorHandler != null && monitorRunnable != null) {
            monitorHandler.removeCallbacks(monitorRunnable);
        }
        overlayManager.hide();
    }

    private void stopBlockingAndService() {
        if (!isBlockingEnabled && monitorThread == null) {
            stopForeground(true);
            stopSelf();
            return;
        }
        stopMonitoringInternal();
        enabledBlockPackages.clear();
        whitelistedPackages.clear();
        cacheLoaded.set(false);
        blockingCount = 0;
        lastBlockedApp = null;
        lastBlockTime = 0L;

        if (monitorThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                monitorThread.quitSafely();
            } else {
                monitorThread.quit();
            }
            monitorThread = null;
            monitorHandler = null;
            monitorRunnable = null;
        }

        stopForeground(true);
        stopSelf();
    }

    private void refreshBlockCacheAsync(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastCacheRefreshMs < CACHE_REFRESH_INTERVAL_MS) {
            return;
        }
        AppExecutors.getInstance().diskIo(() -> {
            try {
                String userId = blockingUserId;
                if (userId == null || userId.isEmpty()) {
                    return;
                }
                List<BlockedApp> all = blockedAppDao.getAllAppsSync(userId);
                Set<String> enabled = new HashSet<>();
                Set<String> whitelisted = new HashSet<>();
                if (all != null) {
                    for (BlockedApp app : all) {
                        if (app.isWhitelisted) {
                            whitelisted.add(app.packageName);
                        } else if (app.isEnabled) {
                            enabled.add(app.packageName);
                        }
                    }
                }
                enabledBlockPackages.clear();
                enabledBlockPackages.addAll(enabled);
                whitelistedPackages.clear();
                whitelistedPackages.addAll(whitelisted);
                lastCacheRefreshMs = System.currentTimeMillis();
                cacheLoaded.set(true);
            } catch (Exception e) {
                AppLog.e(TAG, "Failed to refresh block cache", e);
            }
        });
    }

    private void checkAndBlockApps() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - serviceStartTime > MAX_SERVICE_DURATION) {
            AppLog.w(TAG, "Service timeout, stopping");
            mainHandler.post(this::stopBlockingAndService);
            return;
        }

        if (currentTime - lastCacheRefreshMs >= CACHE_REFRESH_INTERVAL_MS) {
            refreshBlockCacheAsync(false);
        }

        String currentApp = getCurrentAppPackage();
        if (currentApp == null || currentApp.equals(getPackageName())) {
            overlayManager.hide();
            return;
        }

        if (isAppBlocked(currentApp)) {
            if (currentApp.equals(lastBlockedApp)
                    && currentTime - lastBlockTime < MIN_BLOCK_INTERVAL_MS) {
                return;
            }
            blockApp(currentApp);
            lastBlockedApp = currentApp;
            lastBlockTime = currentTime;
            blockingCount++;
        } else {
            overlayManager.hide();
        }
    }

    private String getCurrentAppPackage() {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long end = System.currentTimeMillis();
            long begin = end - Math.max(CHECK_INTERVAL_MS + 500L, 3000L);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.app.usage.UsageEvents events = usageStatsManager.queryEvents(begin, end);
                android.app.usage.UsageEvents.Event event = new android.app.usage.UsageEvents.Event();
                String lastForegroundPackage = null;
                long lastTs = 0L;
                while (events != null && events.hasNextEvent()) {
                    events.getNextEvent(event);
                    if (event.getPackageName() == null) {
                        continue;
                    }
                    int type = event.getEventType();
                    if (type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            && type == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED)) {
                        if (event.getTimeStamp() >= lastTs) {
                            lastTs = event.getTimeStamp();
                            lastForegroundPackage = event.getPackageName();
                        }
                    }
                }
                if (lastForegroundPackage != null) {
                    return lastForegroundPackage;
                }
            }

            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, end - 1000, end);
            if (stats != null && !stats.isEmpty()) {
                UsageStats mostRecent = null;
                for (UsageStats usageStats : stats) {
                    if (mostRecent == null || usageStats.getLastTimeUsed() > mostRecent.getLastTimeUsed()) {
                        mostRecent = usageStats;
                    }
                }
                return mostRecent != null ? mostRecent.getPackageName() : null;
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Error getting current app", e);
        }
        return null;
    }

    private boolean isAppBlocked(String packageName) {
        if (packageName.equals(getPackageName())) {
            return false;
        }
        if (WhitelistManager.isSystemCriticalApp(packageName)) {
            return false;
        }
        if (whitelistedPackages.contains(packageName)) {
            return false;
        }
        if (enabledBlockPackages.contains(packageName)) {
            return true;
        }

        if (!cacheLoaded.get()) {
            return WhitelistManager.shouldBlockByDefault(packageName);
        }

        boolean shouldBlock = WhitelistManager.shouldBlockByDefault(packageName);
        if (shouldBlock) {
            AppExecutors.getInstance().diskIo(() -> {
                try {
                    String userId = blockingUserId;
                    if (userId == null || userId.isEmpty()) {
                        return;
                    }
                    BlockedApp existing = blockedAppDao.getBlockedAppByPackage(userId, packageName);
                    if (existing == null) {
                        BlockedApp blockedApp = AppScanner.buildBlockedApp(this, packageName);
                        blockedApp.userId = userId;
                        blockedAppDao.insert(blockedApp);
                    }
                    enabledBlockPackages.add(packageName);
                } catch (Exception e) {
                    AppLog.e(TAG, "Persist blocked app failed: " + packageName, e);
                }
            });
        }
        return shouldBlock;
    }

    private void blockApp(String packageName) {
        String appName = getAppName(packageName);
        try {
            if (overlayManager.canShowOverlay()) {
                overlayManager.show(appName);
            }
            FocusBlockNavigation.openReturnDestination(this);
        } catch (Exception e) {
            AppLog.e(TAG, "Error blocking app", e);
            try {
                FocusBlockNavigation.openReturnDestination(this);
            } catch (Exception ignored) {
            }
        }
        showBlockingNotification(appName);
    }

    private void showBlockingNotification(String appName) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.focus_block_notification_title))
                    .setContentText(getString(R.string.focus_block_notification_message, appName))
                    .setSmallIcon(R.drawable.ic_timer)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOnlyAlertOnce(true);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify((int) (System.currentTimeMillis() & 0xFFFF), builder.build());
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Error showing blocking notification", e);
        }
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.blocking_channel_focus_mode_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.blocking_channel_focus_mode_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = FocusBlockNavigation.createReturnIntent(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.focus_mode_running_title))
                .setContentText(getString(R.string.focus_mode_running_message))
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void scheduleAutoStop() {
        timeoutHandler.postDelayed(() -> {
            if (isBlockingEnabled) {
                stopBlockingAndService();
            }
        }, MAX_SERVICE_DURATION);
    }

    private void checkServiceTimeout() {
        if (System.currentTimeMillis() - serviceStartTime > MAX_SERVICE_DURATION) {
            stopBlockingAndService();
        }
    }
}
