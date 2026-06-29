package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.service.AppBlockingService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import com.skyinit.pomodorotimer.util.AppLog;
import java.util.List;

/**
 * 应用屏蔽服务工具类
 * 用于检查服务状态和管理服务生命周期
 */
public class AppBlockingServiceUtils {
    private static final String TAG = "AppBlockingServiceUtils";
    
    /**
     * 检查屏蔽服务是否正在运行
     */
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
    
    /**
     * 启动屏蔽服务
     */
    public static void startService(Context context) {
        try {
            Intent intent = new Intent(context, AppBlockingService.class);
            intent.putExtra("action", "start_blocking");
            context.startService(intent);
            AppLog.d(TAG, "Started AppBlockingService");
        } catch (Exception e) {
            AppLog.e(TAG, "Error starting service", e);
        }
    }
    
    /**
     * 停止屏蔽服务
     */
    public static void stopService(Context context) {
        try {
            Intent intent = new Intent(context, AppBlockingService.class);
            intent.putExtra("action", "stop_blocking");
            context.startService(intent);
            AppLog.d(TAG, "Stopped AppBlockingService");
        } catch (Exception e) {
            AppLog.e(TAG, "Error stopping service", e);
        }
    }
    
    /**
     * 确保服务状态与设置同步
     */
    public static void syncServiceStatus(Context context) {
        SettingsManager settings = new SettingsManager(context);
        boolean shouldBeEnabled = settings.isAppBlockingEnabled() && 
                                 PermissionUtils.hasAllAppBlockingPermissions(context);
        boolean isRunning = isServiceRunning(context);
        
        AppLog.d(TAG, "Sync status - shouldBeEnabled: " + shouldBeEnabled + ", isRunning: " + isRunning);
        
        if (shouldBeEnabled && !isRunning) {
            // 应该启用但服务未运行，启动服务
            startService(context);
        } else if (!shouldBeEnabled && isRunning) {
            // 不应该启用但服务正在运行，停止服务
            stopService(context);
        }
    }
}

