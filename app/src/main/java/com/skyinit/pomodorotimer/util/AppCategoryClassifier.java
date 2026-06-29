package com.skyinit.pomodorotimer.util;

import android.content.pm.ApplicationInfo;
import android.os.Build;

/**
 * 应用分类器：精确包名 HashSet 查找 + JSON 规则 + 系统 category 兜底。
 */
public final class AppCategoryClassifier {

    private AppCategoryClassifier() {
    }

    public static String classify(String packageName, String appName) {
        return classify(packageName, appName, null);
    }

    public static String classify(String packageName, String appName, ApplicationInfo appInfo) {
        if (packageName == null || packageName.isEmpty()) {
            return AppCategory.OTHER;
        }
        String safeAppName = appName != null ? appName : packageName;
        AppCategoryRulesLoader loader = AppCategoryRulesLoader.getInstance();

        // 1. O(1) 精确包名匹配
        String exactCategory = loader.getCategoryByExactPackage(packageName);
        if (exactCategory != null) {
            return exactCategory;
        }

        // 2. WhitelistManager 运动健康（精确 + 模糊）
        if (WhitelistManager.isHealthApp(packageName) || WhitelistManager.isOtherHealthApp(packageName)) {
            return AppCategory.HEALTH;
        }

        boolean isSystemApp = isSystemPackage(packageName);

        // 3. JSON 规则按优先级匹配
        for (AppCategoryRulesLoader.CategoryRule rule : loader.getRulesInOrder()) {
            if (AppCategory.SYSTEM.equals(rule.category)) {
                continue;
            }
            if (rule.matches(packageName, safeAppName, isSystemApp)) {
                return rule.category;
            }
        }

        // 4. 求职招聘：work 关键字需排除系统网络类应用
        if (!isSystemApp && packageName.contains("work") && !isSystemWorkPackage(packageName)) {
            return AppCategory.JOB;
        }

        // 5. 厂商系统应用
        if (isVendorSystemApp(packageName)) {
            return AppCategory.SYSTEM;
        }

        // 6. ApplicationInfo.category 系统兜底（无需联网，来自 APK 元数据）
        if (appInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String systemMapped = loader.mapSystemCategory(appInfo.category);
            if (systemMapped != null) {
                return systemMapped;
            }
        }

        return AppCategory.OTHER;
    }

    private static boolean isSystemPackage(String packageName) {
        return packageName.startsWith("com.android.")
                || packageName.startsWith("com.google.android.")
                || packageName.startsWith("com.huawei.")
                || packageName.startsWith("com.miui.")
                || packageName.startsWith("com.xiaomi.")
                || packageName.startsWith("com.hihonor.")
                || packageName.startsWith("cn.honor.")
                || packageName.startsWith("android.")
                || WhitelistManager.isSystemCriticalApp(packageName);
    }

    private static boolean isSystemWorkPackage(String packageName) {
        return packageName.contains("network")
                || packageName.contains("manager")
                || packageName.contains("system")
                || packageName.contains("framework")
                || packageName.contains("service")
                || packageName.contains("provider")
                || packageName.contains("wifi")
                || packageName.contains("bluetooth")
                || packageName.contains("telephony")
                || packageName.contains("connectivity");
    }

    private static boolean isVendorSystemApp(String packageName) {
        if (WhitelistManager.isHonorHuaweiSystemApp(packageName)
                || WhitelistManager.isBrandSystemApp(packageName)) {
            return true;
        }
        if ((packageName.startsWith("com.hihonor.") || packageName.startsWith("cn.honor."))
                && !packageName.equals("com.hihonor.android.launcher")) {
            return true;
        }
        if (packageName.startsWith("com.huawei.")) {
            return true;
        }
        if (packageName.startsWith("com.miui.") || packageName.startsWith("com.xiaomi.")) {
            return true;
        }
        if (packageName.startsWith("com.vivo.")) {
            return true;
        }
        if (packageName.startsWith("com.oppo.")
                || packageName.startsWith("com.coloros.")
                || packageName.startsWith("com.heytap.")) {
            return true;
        }
        if (packageName.startsWith("com.samsung.")) {
            return true;
        }
        return packageName.startsWith("com.oneplus.");
    }
}
