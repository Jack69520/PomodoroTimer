package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import android.content.Context;
import com.skyinit.pomodorotimer.util.AppLog;
import java.util.List;

/**
 * 应用屏蔽功能测试工具类
 */
public class AppBlockingTestUtils {
    private static final String TAG = "AppBlockingTest";
    
    /**
     * 测试白名单逻辑
     */
    public static void testWhitelistLogic(Context context) {
        AppLog.d(TAG, "=== 测试白名单逻辑 ===");
        
        // 测试系统关键应用
        String[] systemApps = {
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone"
        };
        
        for (String packageName : systemApps) {
            boolean shouldBeWhitelisted = WhitelistManager.shouldBeWhitelisted(packageName);
            boolean isSystemCritical = WhitelistManager.isSystemCriticalApp(packageName);
            AppLog.d(TAG, String.format("系统应用 %s: 应白名单=%s, 系统关键=%s", 
                packageName, shouldBeWhitelisted, isSystemCritical));
        }
        
        // 取消“特殊应用”标签：改为验证若干典型应用的默认状态
        String[] typicalApps = {
            "com.eg.android.AlipayGphone", // 支付宝（默认可屏蔽）
            "com.tencent.mm", // 微信（默认可屏蔽）
            "com.tencent.wework", // 企业微信（默认可屏蔽）
            "com.tencent.meeting", // 腾讯会议（默认可屏蔽）
            "com.google.android.youtube", // YouTube（默认可屏蔽）
            "com.xiaomi.market" // 小米应用商店（系统类，默认可屏蔽）
        };
        
        for (String packageName : typicalApps) {
            boolean shouldBeWhitelisted = WhitelistManager.shouldBeWhitelisted(packageName);
            boolean isBrandSystem = WhitelistManager.isBrandSystemApp(packageName);
            AppLog.d(TAG, String.format("应用 %s: 应白名单=%s, 品牌系统=%s", 
                packageName, shouldBeWhitelisted, isBrandSystem));
        }
    }
    
    /**
     * 测试应用扫描
     */
    public static void testAppScanning(Context context) {
        AppLog.d(TAG, "=== 测试应用扫描 ===");
        
        AppScanner scanner = new AppScanner(context);
        List<BlockedApp> apps = scanner.scanInstalledApps();
        
        AppLog.d(TAG, "扫描到 " + apps.size() + " 个应用");
        
        int systemCriticalCount = 0;
        int specialAppCount = 0;
        int whitelistedCount = 0;
        int blockedCount = 0;
        
        for (BlockedApp app : apps) {
            if (WhitelistManager.isSystemCriticalApp(app.packageName)) {
                systemCriticalCount++;
            }
            if (WhitelistManager.isSpecialApp(app.packageName)) {
                specialAppCount++;
            }
            if (app.isWhitelisted) {
                whitelistedCount++;
            }
            if (app.isEnabled) {
                blockedCount++;
            }
        }
        
        AppLog.d(TAG, String.format("统计: 系统关键=%d, 白名单=%d, 已屏蔽=%d", 
            systemCriticalCount, whitelistedCount, blockedCount));
    }
    
    /**
     * 验证强制逻辑：不允许既不加入白名单也不屏蔽
     */
    public static void validateForcedLogic(List<BlockedApp> apps) {
        AppLog.d(TAG, "=== 验证强制逻辑 ===");
        
        int invalidCount = 0;
        for (BlockedApp app : apps) {
            // 检查是否存在既不白名单也不屏蔽的情况
            if (!app.isWhitelisted && !app.isEnabled) {
                AppLog.w(TAG, "发现无效状态: " + app.appName + " 既不白名单也不屏蔽");
                invalidCount++;
            }
        }
        
        if (invalidCount == 0) {
            AppLog.d(TAG, "✅ 所有应用状态都符合强制逻辑");
        } else {
            AppLog.w(TAG, "❌ 发现 " + invalidCount + " 个应用状态不符合强制逻辑");
        }
    }
    
    /**
     * 测试应用类型识别
     */
    public static void testAppTypeRecognition(Context context) {
        AppLog.d(TAG, "=== 测试应用类型识别 ===");
        
        String[] testApps = {
            "com.android.systemui", // 系统关键
            "com.eg.android.AlipayGphone", // 典型可屏蔽应用
            "com.google.android.apps.chrome", // 浏览器
            "com.example.unknown" // 普通应用
        };
        
        for (String packageName : testApps) {
            String appType = WhitelistManager.getAppTypeDescription(context, packageName);
            AppLog.d(TAG, String.format("应用 %s: 类型=%s", packageName, appType));
        }
    }
}
