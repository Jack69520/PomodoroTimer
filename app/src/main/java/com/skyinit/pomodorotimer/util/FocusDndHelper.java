package com.skyinit.pomodorotimer.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.skyinit.pomodorotimer.util.AppLog;

/**
 * 学习计时期间临时启用系统勿扰（DND），结束后恢复先前模式。
 */
public final class FocusDndHelper {

    private static final String TAG = "FocusDndHelper";
    private static Integer savedInterruptionFilter;

    private FocusDndHelper() {
    }

    public static boolean hasPolicyAccess(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    public static Intent createPolicyAccessIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        }
        return new Intent(Settings.ACTION_SETTINGS);
    }

    /** 学习开始时尝试启用 DND；无权限时静默跳过。 */
    public static void maybeEnableDnd(Context context, boolean enabledByUser) {
        if (!enabledByUser || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || !nm.isNotificationPolicyAccessGranted()) {
            AppLog.d(TAG, "DND policy access not granted, skipping enable");
            return;
        }
        if (savedInterruptionFilter == null) {
            savedInterruptionFilter = nm.getCurrentInterruptionFilter();
        }
        try {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        } catch (SecurityException e) {
            AppLog.w(TAG, "Failed to enable DND", e);
        }
    }

    /** 计时结束或中断时恢复先前勿扰模式。 */
    public static void restoreDnd(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || savedInterruptionFilter == null) {
            savedInterruptionFilter = null;
            return;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || !nm.isNotificationPolicyAccessGranted()) {
            savedInterruptionFilter = null;
            return;
        }
        try {
            nm.setInterruptionFilter(savedInterruptionFilter);
        } catch (SecurityException e) {
            AppLog.w(TAG, "Failed to restore DND", e);
        } finally {
            savedInterruptionFilter = null;
        }
    }
}
