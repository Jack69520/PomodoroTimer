package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.service.AppBlockingService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import com.skyinit.pomodorotimer.util.AppLog;
import java.util.List;

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

    public static boolean isServiceRunning(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
            for (ActivityManager.RunningServiceInfo service : services) {
                if (AppBlockingService.class.getName().equals(service.service.getClassName())) {
                    AppLog.d(TAG, "AppBlockingService is running");
                    return true;
                }
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Error checking service status", e);
        }
        return false;
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
        try {
            Intent intent = new Intent(context, AppBlockingService.class);
            intent.putExtra(EXTRA_ACTION, ACTION_REFRESH_CACHE);
            context.startService(intent);
        } catch (Exception e) {
            AppLog.e(TAG, "Error notifying cache refresh", e);
        }
    }

    /**
     * 确保「我的」页独立屏蔽开关与前台服务状态一致。
     */
    public static void syncStandaloneServiceStatus(Context context) {
        boolean shouldBeEnabled = AppContainer.getInstance(context)
                .getUserAppBlockingRepository()
                .isEnabledForCurrentUser()
                && PermissionUtils.hasAllAppBlockingPermissions(context);
        boolean isRunning = isServiceRunning(context);

        AppLog.d(TAG, "Sync standalone - shouldBeEnabled: " + shouldBeEnabled + ", isRunning: " + isRunning);

        if (shouldBeEnabled && !isRunning) {
            startStandaloneBlocking(context);
        } else if (!shouldBeEnabled && isRunning) {
            stopStandaloneBlocking(context);
        }
    }

    private static void sendStart(Context context, String source, long sessionStartTime) {
        try {
            Intent intent = new Intent(context, AppBlockingService.class);
            intent.putExtra(EXTRA_ACTION, ACTION_START_BLOCKING);
            intent.putExtra(EXTRA_BLOCKING_SOURCE, source);
            if (sessionStartTime > 0L) {
                intent.putExtra(EXTRA_SESSION_START_TIME, sessionStartTime);
            }
            context.startService(intent);
            AppLog.d(TAG, "Started AppBlockingService source=" + source);
        } catch (Exception e) {
            AppLog.e(TAG, "Error starting service", e);
        }
    }

    private static void sendStop(Context context, String source) {
        try {
            Intent intent = new Intent(context, AppBlockingService.class);
            intent.putExtra(EXTRA_ACTION, ACTION_STOP_BLOCKING);
            intent.putExtra(EXTRA_BLOCKING_SOURCE, source);
            context.startService(intent);
            AppLog.d(TAG, "Stopped AppBlockingService source=" + source);
        } catch (Exception e) {
            AppLog.e(TAG, "Error stopping service", e);
        }
    }
}
