package com.skyinit.pomodorotimer.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;

/**
 * 锁屏全屏计时能力检测：偏好、会话资格、通知与全屏 Intent 权限。
 * 无状态只读工具，线程安全。
 */
public final class LockScreenTimerGate {

    public enum EnablePrecondition {
        OK,
        NEED_NOTIFICATION,
        NEED_FULL_SCREEN_INTENT,
        NEED_BOTH
    }

    private LockScreenTimerGate() {
    }

    public static boolean isSessionEligible(boolean isRunning, boolean isPaused) {
        return isRunning || isPaused;
    }

    public static boolean hasNotificationPermission(@NonNull Context context) {
        return NotificationPermission.hasNotificationPermission(context);
    }

    /**
     * API 34+ 需用户授予全屏 Intent；更低版本在 Manifest 已声明时视为可用。
     */
    public static boolean canUseFullScreenIntent(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        return nm != null && nm.canUseFullScreenIntent();
    }

    @NonNull
    public static EnablePrecondition evaluateEnablePreconditions(@NonNull Context context) {
        boolean notif = hasNotificationPermission(context);
        boolean fsi = canUseFullScreenIntent(context);
        if (notif && fsi) {
            return EnablePrecondition.OK;
        }
        if (!notif && !fsi) {
            return EnablePrecondition.NEED_BOTH;
        }
        if (!notif) {
            return EnablePrecondition.NEED_NOTIFICATION;
        }
        return EnablePrecondition.NEED_FULL_SCREEN_INTENT;
    }

    /**
     * 缺通知时不可开启；仅缺 FSI 时可降级开启（仍要开启）。
     */
    public static boolean canEnableWithDegradedMode(@NonNull EnablePrecondition precondition) {
        return precondition == EnablePrecondition.NEED_FULL_SCREEN_INTENT
                || precondition == EnablePrecondition.OK;
    }

    public static boolean mustHaveNotification(@NonNull EnablePrecondition precondition) {
        return precondition == EnablePrecondition.NEED_NOTIFICATION
                || precondition == EnablePrecondition.NEED_BOTH;
    }

    @NonNull
    public static Intent createFullScreenIntentSettingsIntent(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            return intent;
        }
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    @NonNull
    public static Intent createNotificationSettingsIntent(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            return intent;
        }
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }
}
