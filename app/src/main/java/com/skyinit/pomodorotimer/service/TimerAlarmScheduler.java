package com.skyinit.pomodorotimer.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.ExactAlarmPermissionHelper;

/**
 * 统一管理计时器相关 Alarm，作为 Handler tick 的兜底。
 * 使用 BroadcastReceiver 触发，避免 Android 12+ 后台直接 startService 受限。
 */
public final class TimerAlarmScheduler {

    private static final String TAG = "TimerAlarmScheduler";

    static final int REQUEST_SESSION_COMPLETE = 2002;
    static final int REQUEST_PAUSE_TIMEOUT = 2001;

    private TimerAlarmScheduler() {
    }

    /** 注册会话到点 Alarm（elapsedRealtime 绝对触发时刻）。 */
    public static void scheduleSessionComplete(Context context, long triggerElapsedRealtime) {
        scheduleExactElapsed(
                context,
                TimerService.ACTION_SESSION_COMPLETE,
                REQUEST_SESSION_COMPLETE,
                triggerElapsedRealtime
        );
    }

    /** 注册暂停超时 Alarm（elapsedRealtime 绝对触发时刻）。 */
    public static void schedulePauseTimeout(Context context, long triggerElapsedRealtime) {
        scheduleExactElapsed(
                context,
                TimerService.ACTION_PAUSE_TIMEOUT,
                REQUEST_PAUSE_TIMEOUT,
                triggerElapsedRealtime
        );
    }

    public static void cancelSessionComplete(Context context) {
        cancel(context, TimerService.ACTION_SESSION_COMPLETE, REQUEST_SESSION_COMPLETE);
    }

    public static void cancelPauseTimeout(Context context) {
        cancel(context, TimerService.ACTION_PAUSE_TIMEOUT, REQUEST_PAUSE_TIMEOUT);
    }

    /** 会话结束或重置时取消全部计时 Alarm。 */
    public static void cancelAll(Context context) {
        cancelSessionComplete(context);
        cancelPauseTimeout(context);
    }

    private static void scheduleExactElapsed(Context context,
                                             String action,
                                             int requestCode,
                                             long triggerElapsedRealtime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildAlarmPendingIntent(context, action, requestCode);
        long safeTrigger = Math.max(triggerElapsedRealtime, SystemClock.elapsedRealtime() + 1000L);

        try {
            if (ExactAlarmPermissionHelper.canScheduleExactAlarms(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            safeTrigger,
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, safeTrigger, pendingIntent);
                }
            } else {
                // 无精确闹钟权限时降级，精度下降但仍有兜底
                AppLog.w(TAG, "Exact alarm not granted, falling back to inexact alarm for " + action);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            safeTrigger,
                            pendingIntent
                    );
                } else {
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, safeTrigger, pendingIntent);
                }
            }
        } catch (SecurityException e) {
            AppLog.e(TAG, "Failed to schedule alarm: " + action, e);
        }
    }

    private static void cancel(Context context, String action, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        alarmManager.cancel(buildAlarmPendingIntent(context, action, requestCode));
    }

    private static PendingIntent buildAlarmPendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, TimerAlarmReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
