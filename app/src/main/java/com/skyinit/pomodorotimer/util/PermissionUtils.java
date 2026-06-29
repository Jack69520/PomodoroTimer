package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.R;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class PermissionUtils {
    private static final String TAG = "PermissionUtils";
    
    /**
     * 检查是否有应用使用统计权限
     */
    public static boolean hasUsageStatsPermission(Context context) {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 
                android.os.Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking usage stats permission", e);
            return false;
        }
    }
    
    /**
     * 请求应用使用统计权限
     */
    public static void requestUsageStatsPermission(Activity activity, int requestCode) {
        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Log.e(TAG, "Error requesting usage stats permission", e);
        }
    }
    
    /**
     * 检查是否有悬浮窗权限
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
    
    /**
     * 请求悬浮窗权限
     */
    public static void requestOverlayPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, requestCode);
        }
    }
    
    /**
     * 检查是否有查询所有应用权限
     */
    public static boolean hasQueryAllPackagesPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return context.checkSelfPermission(android.Manifest.permission.QUERY_ALL_PACKAGES) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    /**
     * 请求查询所有应用权限
     */
    public static void requestQueryAllPackagesPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.requestPermissions(new String[]{android.Manifest.permission.QUERY_ALL_PACKAGES}, requestCode);
        }
    }
    
    /**
     * 检查所有应用屏蔽相关权限
     */
    public static boolean hasAllAppBlockingPermissions(Context context) {
        return hasUsageStatsPermission(context) && 
               hasOverlayPermission(context) && 
               hasQueryAllPackagesPermission(context);
    }
    
    /**
     * 获取缺失的权限描述
     */
    public static String getMissingPermissionDescription(Context context) {
        StringBuilder description = new StringBuilder();
        
        if (!hasUsageStatsPermission(context)) {
            description.append(context.getString(R.string.blocking_permission_usage_stats));
        }
        
        if (!hasOverlayPermission(context)) {
            description.append(context.getString(R.string.blocking_permission_overlay));
        }
        
        if (!hasQueryAllPackagesPermission(context)) {
            description.append(context.getString(R.string.blocking_permission_query_apps));
        }
        
        return description.toString();
    }
}
