package com.skyinit.pomodorotimer.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.service.AppBlockingService;

/**
 * 从设置页或快捷方式启用应用屏蔽的共用流程。
 */
public final class AppBlockingEnabler {

    public static final int REQUEST_USAGE_STATS = 2001;
    public static final int REQUEST_OVERLAY_PERMISSION = 2002;
    public static final int REQUEST_QUERY_ALL_PACKAGES = 2003;

    public interface Host {
        Activity getActivity();

        void onBlockingEnabled();

        void onBlockingEnableFailed();
    }

    private AppBlockingEnabler() {
    }

    /** 若权限齐全则直接启用；否则弹出说明并引导授权。 */
    public static void tryEnable(Activity activity, Host host) {
        if (PermissionUtils.hasAllAppBlockingPermissions(activity)) {
            enableBlocking(activity, host);
        } else {
            showPermissionDialog(activity, host);
        }
    }

    /** 权限页返回后调用。 */
    public static void onPermissionActivityResult(Activity activity, Host host) {
        if (PermissionUtils.hasAllAppBlockingPermissions(activity)) {
            enableBlocking(activity, host);
        } else {
            host.onBlockingEnableFailed();
        }
    }

    /** 按顺序请求缺失权限。 */
    public static void requestNextMissingPermission(Activity activity) {
        if (!PermissionUtils.hasUsageStatsPermission(activity)) {
            PermissionUtils.requestUsageStatsPermission(activity, REQUEST_USAGE_STATS);
        } else if (!PermissionUtils.hasOverlayPermission(activity)) {
            PermissionUtils.requestOverlayPermission(activity, REQUEST_OVERLAY_PERMISSION);
        } else if (!PermissionUtils.hasQueryAllPackagesPermission(activity)) {
            PermissionUtils.requestQueryAllPackagesPermission(activity, REQUEST_QUERY_ALL_PACKAGES);
        }
    }

    private static void showPermissionDialog(Activity activity, Host host) {
        String missing = PermissionUtils.getMissingPermissionDescription(activity);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.common_dialog_permission_title)
                .setMessage(activity.getString(R.string.blocking_dialog_permission_message, missing))
                .setPositiveButton(R.string.confirm, (d, w) -> requestNextMissingPermission(activity))
                .setNegativeButton(R.string.cancel, (d, w) -> host.onBlockingEnableFailed())
                .show();
    }

    private static void enableBlocking(Context context, Host host) {
        new SettingsManager(context).setAppBlockingEnabled(true);
        Intent intent = new Intent(context, AppBlockingService.class);
        intent.putExtra("action", "start_blocking");
        context.startService(intent);
        host.onBlockingEnabled();
    }
}
