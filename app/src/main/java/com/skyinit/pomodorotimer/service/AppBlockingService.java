package com.skyinit.pomodorotimer.service;

import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.BlockedAppDao;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.SessionBlockRecordRepository;
import com.skyinit.pomodorotimer.util.AppBlockingServiceUtils;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyEngine;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyRulesLoader;
import com.skyinit.pomodorotimer.util.AppScanner;
import com.skyinit.pomodorotimer.util.FocusBlockNavigation;
import com.skyinit.pomodorotimer.util.FocusBlockOverlayManager;
import com.skyinit.pomodorotimer.util.PermissionUtils;
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

/**
 * 专注模式应用屏蔽服务。支持「我的」页独立屏蔽与番茄计时联动屏蔽两种来源。
 */
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
    private SessionBlockRecordRepository blockRecordRepository;
    private FocusBlockOverlayManager overlayManager;

    private final Set<String> enabledBlockPackages = ConcurrentHashMap.newKeySet();
    private final Set<String> whitelistedPackages = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean cacheLoaded = new AtomicBoolean(false);
    private long lastCacheRefreshMs;

    private boolean standaloneBlockingActive;
    private boolean timerBlockingActive;
    private long timerSessionStartTime;

    private boolean isMonitoring;
    /** 本次屏蔽服务固定绑定的账户，避免账户切换后继续使用其他账户规则。 */
    private String blockingUserId;
    private long serviceStartTime;
    private String lastBlockedApp;
    private long lastBlockTime;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        timeoutHandler = new Handler(Looper.getMainLooper());
        overlayManager = new FocusBlockOverlayManager(this);
        blockedAppDao = AppDatabase.getDatabase(this).blockedAppDao();
        blockRecordRepository = AppContainer.getInstance(this).getSessionBlockRecordRepository();
        serviceStartTime = System.currentTimeMillis();
        createNotificationChannel();
    }

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
            String action = intent.getStringExtra(AppBlockingServiceUtils.EXTRA_ACTION);
            if (AppBlockingServiceUtils.ACTION_START_BLOCKING.equals(action)) {
                handleStartBlocking(intent);
            } else if (AppBlockingServiceUtils.ACTION_STOP_BLOCKING.equals(action)) {
                handleStopBlocking(intent);
            } else if (AppBlockingServiceUtils.ACTION_REFRESH_CACHE.equals(action)) {
                if (isMonitoring) {
                    refreshBlockCacheAsync(true);
                }
            } else if ("check_timeout".equals(action)) {
                checkServiceTimeout();
            }
        }
        if (!isAnyBlockingActive()) {
            stopSelf();
        }
        return isAnyBlockingActive() ? START_STICKY : START_NOT_STICKY;
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

    private void handleStartBlocking(Intent intent) {
        if (!PermissionUtils.hasUsageStatsPermission(this)) {
            AppLog.w(TAG, "Usage stats permission not granted");
            return;
        }

        String source = intent.getStringExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE);
        if (AppBlockingServiceUtils.SOURCE_TIMER.equals(source)) {
            timerBlockingActive = true;
            timerSessionStartTime = intent.getLongExtra(
                    AppBlockingServiceUtils.EXTRA_SESSION_START_TIME, 0L);
        } else {
            standaloneBlockingActive = true;
        }
        ensureMonitoringStarted();
    }

    private void handleStopBlocking(Intent intent) {
        String source = intent.getStringExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE);
        if (AppBlockingServiceUtils.SOURCE_TIMER.equals(source)) {
            timerBlockingActive = false;
            timerSessionStartTime = 0L;
        } else {
            standaloneBlockingActive = false;
        }
        if (!isAnyBlockingActive()) {
            stopBlockingAndService();
        }
    }

    private boolean isAnyBlockingActive() {
        return standaloneBlockingActive || timerBlockingActive;
    }

    private void ensureMonitoringStarted() {
        String activeUserId = AccountManager.getInstance(this).requireActiveUserId();
        if (activeUserId == null || activeUserId.isEmpty()) {
            AppLog.w(TAG, "Cannot start blocking without active user");
            standaloneBlockingActive = false;
            timerBlockingActive = false;
            return;
        }

        if (!isMonitoring) {
            blockingUserId = activeUserId;
            isMonitoring = true;
            serviceStartTime = System.currentTimeMillis();
            startBlockingForeground();
            scheduleAutoStop();
            lastBlockedApp = null;
            lastBlockTime = 0L;
            cacheLoaded.set(false);
            refreshBlockCacheAsync(true);
            startMonitorLoop();
            AppLog.d(TAG, "App blocking monitoring started");
        } else if (!activeUserId.equals(blockingUserId)) {
            blockingUserId = activeUserId;
            cacheLoaded.set(false);
            refreshBlockCacheAsync(true);
        }
    }

    private void startMonitorLoop() {
        if (monitorThread == null) {
            monitorThread = new HandlerThread("AppBlockingMonitor");
            monitorThread.start();
            monitorHandler = new Handler(monitorThread.getLooper());
        }

        monitorRunnable = () -> {
            try {
                if (isMonitoring && isAnyBlockingActive()) {
                    checkAndBlockApps();
                }
            } catch (Throwable t) {
                AppLog.e(TAG, "Monitor error", t);
            } finally {
                if (isMonitoring && isAnyBlockingActive()
                        && monitorHandler != null && monitorRunnable != null) {
                    monitorHandler.postDelayed(monitorRunnable, CHECK_INTERVAL_MS);
                }
            }
        };
        monitorHandler.post(monitorRunnable);
    }

    private void stopMonitoringInternal() {
        isMonitoring = false;
        blockingUserId = null;
        if (monitorHandler != null && monitorRunnable != null) {
            monitorHandler.removeCallbacks(monitorRunnable);
        }
        overlayManager.hide();
    }

    private void stopBlockingAndService() {
        standaloneBlockingActive = false;
        timerBlockingActive = false;
        timerSessionStartTime = 0L;

        if (!isMonitoring && monitorThread == null) {
            stopForeground(true);
            stopSelf();
            return;
        }
        stopMonitoringInternal();
        enabledBlockPackages.clear();
        whitelistedPackages.clear();
        cacheLoaded.set(false);
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

    /** 系统分区标志缓存，避免监控循环频繁查 PackageManager。 */
    private final ConcurrentHashMap<String, Boolean> systemPartitionCache = new ConcurrentHashMap<>();

    /**
     * 判定是否应拦截：缓存未就绪时一律放行；
     * 未入库的系统分区包一律不拦（隐藏=不管）；第三方未知包可按默认策略拦并异步入库。
     */
    private boolean isAppBlocked(String packageName) {
        if (!cacheLoaded.get()) {
            return false;
        }

        BlockingPolicyEngine policy = BlockingPolicyRulesLoader.getInstance().createEngine();

        if (packageName.equals(getPackageName())) {
            return false;
        }
        if (policy.isCritical(packageName)) {
            return false;
        }
        if (whitelistedPackages.contains(packageName)) {
            return false;
        }
        if (enabledBlockPackages.contains(packageName)) {
            return true;
        }

        // 未入库：系统分区 → 不拦不插入；第三方 → 可按默认策略拦并补入库
        if (isSystemPartitionPackage(packageName)) {
            return false;
        }

        boolean shouldBlock = policy.shouldBlockByDefault(packageName);
        if (shouldBlock) {
            AppExecutors.getInstance().diskIo(() -> {
                try {
                    String userId = blockingUserId;
                    if (userId == null || userId.isEmpty()) {
                        return;
                    }
                    BlockedApp existing = blockedAppDao.getBlockedAppByPackage(userId, packageName);
                    if (existing == null) {
                        BlockedApp blockedApp = AppScanner.buildBlockedApp(this, packageName, policy);
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

    private boolean isSystemPartitionPackage(String packageName) {
        Boolean cached = systemPartitionCache.get(packageName);
        if (cached != null) {
            return cached;
        }
        boolean isSystem = false;
        try {
            ApplicationInfo info = getPackageManager()
                    .getApplicationInfo(packageName, 0);
            isSystem = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (Exception e) {
            // 查不到时偏向不拦（不当第三方默认拦）
            isSystem = true;
        }
        systemPartitionCache.put(packageName, isSystem);
        return isSystem;
    }

    private void blockApp(String packageName) {
        String appName = getAppName(packageName);
        long blockTime = System.currentTimeMillis();
        recordBlockEventIfNeeded(packageName, appName, blockTime);

        try {
            if (overlayManager.canShowOverlay()) {
                overlayManager.show(appName);
            }
            FocusBlockNavigation.openReturnDestination(this);
        } catch (Exception e) {
            AppLog.e(TAG, "Error blocking app", e);
            try {
                FocusBlockNavigation.openReturnDestination(this);
            } catch (Exception fallbackError) {
                AppLog.w(TAG, "Fallback navigation after block failure also failed", fallbackError);
            }
        }
        showBlockingNotification(appName);
    }

    private void recordBlockEventIfNeeded(String packageName, String appName, long blockTime) {
        if (!timerBlockingActive || timerSessionStartTime <= 0L) {
            return;
        }
        String userId = blockingUserId;
        if (userId == null || userId.isEmpty()) {
            return;
        }
        final long sessionStart = timerSessionStartTime;
        AppExecutors.getInstance().diskIo(() ->
                blockRecordRepository.recordBlockSync(userId, sessionStart, packageName, appName, blockTime));
    }

    private void showBlockingNotification(String appName) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.focus_block_notification_title))
                    .setContentText(getString(R.string.focus_block_notification_message, appName))
                    .setSmallIcon(R.drawable.ic_timer)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        timeoutHandler.removeCallbacksAndMessages(null);
        timeoutHandler.postDelayed(() -> {
            if (isMonitoring) {
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
