package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyEngine;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫描设备已安装应用并构建 {@link BlockedApp} 列表。
 * 屏蔽策略由注入的 {@link BlockingPolicyEngine} 决定，不再硬编码规则。
 */
public class AppScanner {
    private static final String TAG = "AppScanner";
    private final Context context;
    private final PackageManager packageManager;
    private final BlockingPolicyEngine policyEngine;

    public AppScanner(Context context, BlockingPolicyEngine policyEngine) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        this.policyEngine = policyEngine;
    }

    public List<BlockedApp> scanInstalledApps() {
        List<BlockedApp> apps = new ArrayList<>();

        try {
            List<ApplicationInfo> installedApps =
                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo appInfo : installedApps) {
                if (shouldIncludeApp(appInfo)) {
                    apps.add(createBlockedAppFromAppInfo(appInfo));
                }
            }

            AppLog.d(TAG, "Scanned " + apps.size() + " installed apps");
        } catch (Exception e) {
            AppLog.e(TAG, "Error scanning installed apps", e);
        }

        return apps;
    }

    private boolean shouldIncludeApp(ApplicationInfo appInfo) {
        String packageName = appInfo.packageName;
        if (packageName.equals(context.getPackageName())) {
            return false;
        }

        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return policyEngine.shouldIncludeSystemApp(packageName);
        }

        return true;
    }

    private BlockedApp createBlockedAppFromAppInfo(ApplicationInfo appInfo) {
        String appName = packageManager.getApplicationLabel(appInfo).toString();
        String category = categorizeApp(appInfo.packageName, appName, appInfo);

        BlockedApp blockedApp = new BlockedApp(appInfo.packageName, appName, category);
        policyEngine.applyDefaultPolicy(blockedApp);

        return blockedApp;
    }

    public static String categorizeApp(String packageName, String appName) {
        return categorizeApp(packageName, appName, null);
    }

    public static String categorizeApp(String packageName, String appName, ApplicationInfo appInfo) {
        return AppCategoryClassifier.classify(packageName, appName, appInfo);
    }

    public static BlockedApp buildBlockedApp(Context context,
                                             String packageName,
                                             BlockingPolicyEngine policyEngine) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            String appName = pm.getApplicationLabel(appInfo).toString();
            String category = categorizeApp(packageName, appName, appInfo);

            BlockedApp blockedApp = new BlockedApp(packageName, appName, category);
            policyEngine.applyDefaultPolicy(blockedApp);

            return blockedApp;
        } catch (Exception e) {
            BlockedApp blockedApp = new BlockedApp(packageName, packageName, AppCategory.OTHER);
            blockedApp.isWhitelisted = false;
            blockedApp.isEnabled = true;
            return blockedApp;
        }
    }
}
