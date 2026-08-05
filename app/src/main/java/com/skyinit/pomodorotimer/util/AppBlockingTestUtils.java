package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.domain.blocking.AppTypeLabelResolver;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyEngine;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyRulesLoader;

import android.content.Context;

import java.util.List;

/**
 * 应用屏蔽功能测试工具类（开发期日志验证）。
 */
public class AppBlockingTestUtils {
    private static final String TAG = "AppBlockingTest";

    public static void testPolicyLogic(Context context) {
        BlockingPolicyEngine policy = BlockingPolicyRulesLoader.getInstance().createEngine();
        AppLog.d(TAG, "=== 测试屏蔽策略 ===");

        String[] systemApps = {
                "com.android.systemui",
                "com.android.settings",
                "com.android.phone"
        };

        for (String packageName : systemApps) {
            boolean shouldBeWhitelisted = policy.shouldBeWhitelisted(packageName);
            boolean isCritical = policy.isCritical(packageName);
            AppLog.d(TAG, String.format("平台关键 %s: 应白名单=%s, CRITICAL=%s",
                    packageName, shouldBeWhitelisted, isCritical));
        }

        String[] typicalApps = {
                "com.eg.android.AlipayGphone",
                "com.tencent.mm",
                "com.tencent.wework",
                "com.tencent.meeting",
                "com.google.android.youtube",
                "com.xiaomi.market"
        };

        for (String packageName : typicalApps) {
            boolean shouldBeWhitelisted = policy.shouldBeWhitelisted(packageName);
            boolean shouldBlock = policy.shouldBlockByDefault(packageName);
            AppLog.d(TAG, String.format("应用 %s: 应白名单=%s, 默认屏蔽=%s",
                    packageName, shouldBeWhitelisted, shouldBlock));
        }
    }

    public static void testAppScanning(Context context) {
        AppLog.d(TAG, "=== 测试应用扫描 ===");
        BlockingPolicyEngine policy = BlockingPolicyRulesLoader.getInstance().createEngine();
        AppScanner scanner = new AppScanner(context, policy);
        List<BlockedApp> apps = scanner.scanInstalledApps();

        AppLog.d(TAG, "扫描到 " + apps.size() + " 个应用");

        int systemCriticalCount = 0;
        int whitelistedCount = 0;
        int blockedCount = 0;

        for (BlockedApp app : apps) {
            if (policy.isCritical(app.packageName)) {
                systemCriticalCount++;
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

    public static void validateForcedLogic(List<BlockedApp> apps) {
        AppLog.d(TAG, "=== 验证强制逻辑 ===");

        int invalidCount = 0;
        for (BlockedApp app : apps) {
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

    public static void testAppTypeRecognition(Context context) {
        AppLog.d(TAG, "=== 测试应用类型识别 ===");
        AppTypeLabelResolver resolver = new AppTypeLabelResolver(
                BlockingPolicyRulesLoader.getInstance().getConfig());

        String[] testApps = {
                "com.android.systemui",
                "com.eg.android.AlipayGphone",
                "com.google.android.apps.chrome",
                "com.example.unknown"
        };

        for (String packageName : testApps) {
            String appType = resolver.resolve(context, packageName);
            AppLog.d(TAG, String.format("应用 %s: 类型=%s", packageName, appType));
        }
    }
}
