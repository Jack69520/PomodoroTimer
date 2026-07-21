package com.skyinit.pomodorotimer.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 设置页系统权限检测与跳转。特殊权限（精确闹钟 / 使用情况 / 悬浮窗）走系统设置页；
 * 其余运行时权限可先 request，失败后再打开应用详情。
 */
public final class SettingsPermissionHelper {

    public enum Kind {
        NOTIFICATION,
        /** API 32 及以下：统一的存储/媒体访问 */
        MEDIA_FILES,
        /** API 33+：音乐与音频 */
        MUSIC_AUDIO,
        /** API 33+：照片（本应用声明 READ_MEDIA_IMAGES） */
        PHOTOS_VIDEOS,
        CAMERA,
        EXACT_ALARM,
        /** 应用屏蔽：使用情况访问 */
        USAGE_STATS_BLOCKING,
        /** 应用屏蔽：悬浮窗 */
        OVERLAY_BLOCKING
    }

    private SettingsPermissionHelper() {
    }

    /** 当前系统版本是否需要展示该项。 */
    public static boolean isApplicable(@NonNull Kind kind) {
        switch (kind) {
            case NOTIFICATION:
                return true;
            case MEDIA_FILES:
                return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU;
            case MUSIC_AUDIO:
            case PHOTOS_VIDEOS:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
            case CAMERA:
                return true;
            case EXACT_ALARM:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
            case USAGE_STATS_BLOCKING:
            case OVERLAY_BLOCKING:
                return true;
            default:
                return false;
        }
    }

    public static boolean isGranted(@NonNull Context context, @NonNull Kind kind) {
        if (!isApplicable(kind)) {
            return true;
        }
        switch (kind) {
            case NOTIFICATION:
                return NotificationPermission.hasNotificationPermission(context);
            case MEDIA_FILES:
                return hasRuntimePermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            case MUSIC_AUDIO:
                return hasRuntimePermission(context, Manifest.permission.READ_MEDIA_AUDIO);
            case PHOTOS_VIDEOS:
                return hasRuntimePermission(context, Manifest.permission.READ_MEDIA_IMAGES);
            case CAMERA:
                return hasRuntimePermission(context, Manifest.permission.CAMERA);
            case EXACT_ALARM:
                return ExactAlarmPermissionHelper.canScheduleExactAlarms(context);
            case USAGE_STATS_BLOCKING:
                return PermissionUtils.hasUsageStatsPermission(context);
            case OVERLAY_BLOCKING:
                return PermissionUtils.hasOverlayPermission(context);
            default:
                return false;
        }
    }

    /**
     * 返回可直接 {@code requestPermissions} 的权限名；特殊权限返回 null，需走 {@link #openPermissionSettings}。
     */
    @Nullable
    public static String getRuntimePermission(@NonNull Kind kind) {
        if (!isApplicable(kind)) {
            return null;
        }
        switch (kind) {
            case NOTIFICATION:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? Manifest.permission.POST_NOTIFICATIONS
                        : null;
            case MEDIA_FILES:
                return Manifest.permission.READ_EXTERNAL_STORAGE;
            case MUSIC_AUDIO:
                return Manifest.permission.READ_MEDIA_AUDIO;
            case PHOTOS_VIDEOS:
                return Manifest.permission.READ_MEDIA_IMAGES;
            case CAMERA:
                return Manifest.permission.CAMERA;
            default:
                return null;
        }
    }

    /** 是否只能通过系统设置页授权（无法弹出运行时权限对话框）。 */
    public static boolean requiresSettingsIntent(@NonNull Kind kind) {
        switch (kind) {
            case EXACT_ALARM:
            case USAGE_STATS_BLOCKING:
            case OVERLAY_BLOCKING:
                return true;
            case NOTIFICATION:
                return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU;
            default:
                return getRuntimePermission(kind) == null;
        }
    }

    /**
     * 打开与该权限相关的系统设置页。无法精确定位时回退到应用详情页。
     *
     * @return true 表示已成功发起跳转
     */
    public static boolean openPermissionSettings(@NonNull Activity activity, @NonNull Kind kind) {
        try {
            Intent intent = createSettingsIntent(activity, kind);
            activity.startActivity(intent);
            return true;
        } catch (Exception primary) {
            try {
                activity.startActivity(createAppDetailsIntent(activity));
                return true;
            } catch (Exception fallback) {
                return false;
            }
        }
    }

    @NonNull
    public static Intent createSettingsIntent(@NonNull Context context, @NonNull Kind kind) {
        switch (kind) {
            case EXACT_ALARM:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent exact = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    exact.setData(Uri.parse("package:" + context.getPackageName()));
                    return exact;
                }
                break;
            case USAGE_STATS_BLOCKING:
                return new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            case OVERLAY_BLOCKING:
                Intent overlay = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                overlay.setData(Uri.parse("package:" + context.getPackageName()));
                return overlay;
            case NOTIFICATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent notification = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    notification.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                    return notification;
                }
                break;
            default:
                break;
        }
        return createAppDetailsIntent(context);
    }

    @NonNull
    public static Intent createAppDetailsIntent(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    /**
     * 是否应直接打开设置页（用户曾拒绝且系统不再弹出授权框）。
     */
    public static boolean shouldOpenSettingsDirectly(
            @NonNull Activity activity,
            @NonNull Kind kind) {
        if (requiresSettingsIntent(kind)) {
            return true;
        }
        String permission = getRuntimePermission(kind);
        if (permission == null) {
            return true;
        }
        if (isGranted(activity, kind)) {
            return false;
        }
        // 从未请求过：应先弹系统对话框
        // 已拒绝且不再显示 rationale：视为永久拒绝，直接进设置
        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
        return !showRationale && wasRequestedBefore(activity, permission);
    }

    public static void markRequested(@NonNull Context context, @NonNull String permission) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(keyRequested(permission), true)
                .apply();
    }

    public static boolean wasRequestedBefore(@NonNull Context context, @NonNull String permission) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(keyRequested(permission), false);
    }

    private static boolean hasRuntimePermission(@NonNull Context context, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static final String PREFS = "settings_permission_prefs";

    private static String keyRequested(String permission) {
        return "requested_" + permission;
    }
}
