package com.skyinit.pomodorotimer.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * 统一启动 TimerService。应用处于前台时使用 startForegroundService；否则使用 startService，
 * 避免 Android 12+ 在后台启动前台服务时崩溃。
 */
public final class TimerServiceLauncher {

    private TimerServiceLauncher() {
    }

    public static void ensureRunning(Context context) {
        startServiceCompat(context, new Intent(context, TimerService.class));
    }

    public static void deliverAction(Context context, String action) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(action);
        deliverAction(context, intent);
    }

    /** 携带 Intent extras 启动（如暂停原因）。 */
    public static void deliverAction(Context context, Intent intent) {
        if (intent.getComponent() == null) {
            intent.setClass(context, TimerService.class);
        }
        startServiceCompat(context, intent);
    }

    private static void startServiceCompat(Context context, Intent intent) {
        Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isAppInForeground(appContext)) {
            try {
                ContextCompat.startForegroundService(appContext, intent);
                return;
            } catch (IllegalStateException e) {
                // 部分机型在边界状态下仍可能拒绝前台服务启动，降级为普通 startService。
            }
        }
        appContext.startService(intent);
    }

    public static boolean isAppInForeground(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
            if (!packageName.equals(processInfo.processName)) {
                continue;
            }
            int importance = processInfo.importance;
            return importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    || importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
        }
        return false;
    }
}
