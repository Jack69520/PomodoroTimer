package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.domain.appidentity.AppIdentityRulesLoader;
import com.skyinit.pomodorotimer.domain.appidentity.AppProvenance;
import com.skyinit.pomodorotimer.domain.appidentity.AppProvenanceResolver;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyEngine;
import com.skyinit.pomodorotimer.domain.blocking.BlockingRole;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫描设备已安装应用并构建 {@link BlockedApp} 列表。
 * 来源身份、屏蔽角色、业务分类三轴解耦写入实体。
 */
public class AppScanner {
    private static final String TAG = "AppScanner";
    private final Context context;
    private final PackageManager packageManager;
    private final BlockingPolicyEngine policyEngine;
    private final AppProvenanceResolver provenanceResolver;

    public AppScanner(Context context, BlockingPolicyEngine policyEngine) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        this.policyEngine = policyEngine;
        this.provenanceResolver = AppIdentityRulesLoader.getInstance().createResolver();
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
            return policyEngine.shouldIncludeInManagedList(packageName);
        }

        return true;
    }

    private BlockedApp createBlockedAppFromAppInfo(ApplicationInfo appInfo) {
        String packageName = appInfo.packageName;
        String appName = packageManager.getApplicationLabel(appInfo).toString();
        boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean hasLauncher = hasLauncherActivity(packageName);
        AppProvenance provenance = provenanceResolver.resolve(packageName, isSystem, hasLauncher);
        String category = AppCategoryClassifier.classify(packageName, appName, appInfo, provenance);

        BlockedApp blockedApp = new BlockedApp(packageName, appName, category);
        blockedApp.provenance = provenance.toStorage();
        policyEngine.applyDefaultPolicy(blockedApp);
        return blockedApp;
    }

    private boolean hasLauncherActivity(String packageName) {
        try {
            Intent launch = packageManager.getLaunchIntentForPackage(packageName);
            return launch != null;
        } catch (Exception e) {
            return false;
        }
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
            boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean hasLauncher = pm.getLaunchIntentForPackage(packageName) != null;
            AppProvenance provenance = AppIdentityRulesLoader.getInstance()
                    .createResolver()
                    .resolve(packageName, isSystem, hasLauncher);
            String category = AppCategoryClassifier.classify(packageName, appName, appInfo, provenance);

            BlockedApp blockedApp = new BlockedApp(packageName, appName, category);
            blockedApp.provenance = provenance.toStorage();
            policyEngine.applyDefaultPolicy(blockedApp);
            return blockedApp;
        } catch (Exception e) {
            BlockedApp blockedApp = new BlockedApp(packageName, packageName, AppCategory.OTHER);
            blockedApp.provenance = AppProvenance.THIRD_PARTY.toStorage();
            blockedApp.blockingRole = BlockingRole.DEFAULT_BLOCK.toStorage();
            blockedApp.isWhitelisted = false;
            blockedApp.isEnabled = true;
            return blockedApp;
        }
    }
}
