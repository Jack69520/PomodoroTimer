package com.skyinit.pomodorotimer.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.AccountManager;

/**
 * 从我的页或快捷方式启用应用屏蔽的共用流程。
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

    /** 权限页返回后调用：已齐则启用；当前步已授则继续下一项；否则失败。 */
    public static void onPermissionActivityResult(Activity activity, Host host, int requestCode) {
        if (PermissionUtils.hasAllAppBlockingPermissions(activity)) {
            enableBlocking(activity, host);
            return;
        }
        if (!wasRequestedPermissionGranted(activity, requestCode)) {
            host.onBlockingEnableFailed();
            return;
        }
        // 当前步骤已授权，继续引导下一项缺失权限（避免首项完成后误报失败）。
        requestNextMissingPermission(activity);
    }

    private static boolean wasRequestedPermissionGranted(Activity activity, int requestCode) {
        if (requestCode == REQUEST_USAGE_STATS) {
            return PermissionUtils.hasUsageStatsPermission(activity);
        }
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            return PermissionUtils.hasOverlayPermission(activity);
        }
        if (requestCode == REQUEST_QUERY_ALL_PACKAGES) {
            return PermissionUtils.hasQueryAllPackagesPermission(activity);
        }
        return false;
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
        if (!AccountManager.getInstance(context).hasActiveSession()) {
            host.onBlockingEnableFailed();
            return;
        }
        AppContainer.getInstance(context)
                .getUserAppBlockingRepository()
                .setEnabledForCurrentUser(true);
        AppBlockingServiceUtils.startStandaloneBlocking(context);
        host.onBlockingEnabled();
    }
}
