package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class AppScanner {
    private static final String TAG = "AppScanner";
    private final Context context;
    private final PackageManager packageManager;

    public AppScanner(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
    }

    /**
     * 扫描设备上所有已安装的应用
     */
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
            return WhitelistManager.shouldIncludeSystemApp(packageName);
        }

        return true;
    }

    private BlockedApp createBlockedAppFromAppInfo(ApplicationInfo appInfo) {
        String appName = packageManager.getApplicationLabel(appInfo).toString();
        String category = categorizeApp(appInfo.packageName, appName, appInfo);

        BlockedApp blockedApp = new BlockedApp(appInfo.packageName, appName, category);
        applyDefaultBlockingPolicy(blockedApp);

        return blockedApp;
    }

    private static void applyDefaultBlockingPolicy(BlockedApp blockedApp) {
        blockedApp.isWhitelisted = WhitelistManager.shouldBeWhitelisted(blockedApp.packageName);
        blockedApp.isEnabled = WhitelistManager.shouldBlockByDefault(blockedApp.packageName);
    }

    public static String categorizeApp(String packageName, String appName) {
        return categorizeApp(packageName, appName, null);
    }

    public static String categorizeApp(String packageName, String appName, ApplicationInfo appInfo) {
        return AppCategoryClassifier.classify(packageName, appName, appInfo);
    }

    /**
     * 基于包名构建一个 BlockedApp（用于服务在发现未入库应用时快速入库）
     */
    public static BlockedApp buildBlockedApp(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            String appName = pm.getApplicationLabel(appInfo).toString();
            String category = categorizeApp(packageName, appName, appInfo);

            BlockedApp blockedApp = new BlockedApp(packageName, appName, category);
            applyDefaultBlockingPolicy(blockedApp);

            return blockedApp;
        } catch (Exception e) {
            BlockedApp blockedApp = new BlockedApp(packageName, packageName, AppCategory.OTHER);
            blockedApp.isWhitelisted = false;
            blockedApp.isEnabled = true;
            return blockedApp;
        }
    }
}
