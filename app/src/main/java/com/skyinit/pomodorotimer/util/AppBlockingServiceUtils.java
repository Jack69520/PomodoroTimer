package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.content.Intent;

import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.domain.blocking.BlockingRuntimeState;
import com.skyinit.pomodorotimer.service.AppBlockingService;

/**
 * 应用屏蔽服务工具类：区分计时联动与独立屏蔽两种来源，管理服务生命周期与缓存刷新。
 */
public final class AppBlockingServiceUtils {
    private static final String TAG = "AppBlockingServiceUtils";

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_BLOCKING_SOURCE = "blocking_source";
    public static final String EXTRA_SESSION_START_TIME = "session_start_time";

    public static final String ACTION_START_BLOCKING = "start_blocking";
    public static final String ACTION_STOP_BLOCKING = "stop_blocking";
    public static final String ACTION_REFRESH_CACHE = "refresh_cache";

    public static final String SOURCE_STANDALONE = "standalone";
    public static final String SOURCE_TIMER = "timer";

    private AppBlockingServiceUtils() {
    }

    /** 进程内运行态：是否正在监控（L3）。 */
    public static boolean isServiceRunning(Context context) {
        return BlockingRuntimeState.isMonitoringActive();
    }

    public static boolean isStandaloneActive() {
        return BlockingRuntimeState.isStandaloneActive();
    }

    public static boolean isTimerActive() {
        return BlockingRuntimeState.isTimerActive();
    }

    /**
     * 独立屏蔽 orphan：意图 ON、权限齐、独立源未激活。
     * 调用方须保证 prefs 缓存已 warm。
     */
    public static boolean isStandaloneOrphan(Context context) {
        if (!PermissionUtils.hasAllAppBlockingPermissions(context)) {
            return false;
        }
        boolean prefsEnabled = AppContainer.getInstance(context)
                .getUserAppBlockingRepository()
                .isEnabledForCurrentUser();
        return prefsEnabled && !BlockingRuntimeState.isStandaloneActive();
    }

    public static void startStandaloneBlocking(Context context) {
        sendStart(context, SOURCE_STANDALONE, 0L);
    }

    public static void stopStandaloneBlocking(Context context) {
        sendStop(context, SOURCE_STANDALONE);
    }

    public static void startTimerBlocking(Context context, long sessionStartTime) {
        sendStart(context, SOURCE_TIMER, sessionStartTime);
    }

    public static void stopTimerBlocking(Context context) {
        sendStop(context, SOURCE_TIMER);
    }

    public static void notifyCacheRefresh(Context context) {
        if (!BlockingRuntimeState.isMonitoringActive()) {
            AppLog.d(TAG, "Skip cache refresh: not monitoring");
            return;
        }
        try {
            Intent intent = new Intent(context, AppBlockingService.class);
            intent.putExtra(EXTRA_ACTION, ACTION_REFRESH_CACHE);
            context.startService(intent);
        } catch (Exception e) {
            AppLog.e(TAG, "Error notifying cache refresh", e);
        }
    }

    /**
     * @deprecated 已由 {@link StandaloneBlockingReconciler} 取代；保留空实现防止旧调用静默拉起。
     */
    @Deprecated
    public static void syncStandaloneServiceStatus(Context context) {
        AppLog.d(TAG, "syncStandaloneServiceStatus is no-op; use StandaloneBlockingReconciler");
    }

    private static void sendStart(Context context, String source, long sessionStartTime) {
        try {
            Intent intent = new Intent(context, AppBlockingService.class);
            intent.putExtra(EXTRA_ACTION, ACTION_START_BLOCKING);
            intent.putExtra(EXTRA_BLOCKING_SOURCE, source);
            if (sessionStartTime > 0L) {
                intent.putExtra(EXTRA_SESSION_START_TIME, sessionStartTime);
            }
            // 使用 startService：权限不足时 handleStart 可能不进前台，避免 startForegroundService 超时 ANR
            context.getApplicationContext().startService(intent);
            AppLog.d(TAG, "Started AppBlockingService source=" + source);
        } catch (Exception e) {
            AppLog.e(TAG, "Error starting service", e);
        }
    }

    private static void sendStop(Context context, String source) {
        // 始终下发 stop：L3 可能短暂不同步；Service 侧幂等处理无源情况
        try {
            Intent intent = new Intent(context, AppBlockingService.class);
            intent.putExtra(EXTRA_ACTION, ACTION_STOP_BLOCKING);
            intent.putExtra(EXTRA_BLOCKING_SOURCE, source);
            context.getApplicationContext().startService(intent);
            AppLog.d(TAG, "Stopped AppBlockingService source=" + source);
        } catch (Exception e) {
            AppLog.e(TAG, "Error stopping service", e);
        }
    }
}
