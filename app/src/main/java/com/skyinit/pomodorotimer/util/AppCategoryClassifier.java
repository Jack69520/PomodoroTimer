package com.skyinit.pomodorotimer.util;

import android.content.pm.ApplicationInfo;
import android.os.Build;

/**
 * 应用分类器：精确包名 HashSet 查找 + JSON 规则 + 系统 category 兜底。
 * 分类逻辑与屏蔽策略完全解耦。
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

        String exactCategory = loader.getCategoryByExactPackage(packageName);
        if (exactCategory != null) {
            return exactCategory;
        }

        boolean isSystemApp = isSystemPackage(packageName);

        for (AppCategoryRulesLoader.CategoryRule rule : loader.getRulesInOrder()) {
            if (AppCategory.SYSTEM.equals(rule.category)) {
                continue;
            }
            if (rule.matches(packageName, safeAppName, isSystemApp)) {
                return rule.category;
            }
        }

        if (!isSystemApp && packageName.contains("work") && !isSystemWorkPackage(packageName)) {
            return AppCategory.JOB;
        }

        if (isVendorSystemApp(packageName)) {
            return AppCategory.SYSTEM;
        }

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
                || packageName.startsWith("android.");
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
        if (packageName.startsWith("com.vivo.") || packageName.startsWith("com.bbk.")) {
            return true;
        }
        if (packageName.startsWith("com.oppo.")
                || packageName.startsWith("com.coloros.")
                || packageName.startsWith("com.heytap.")
                || packageName.startsWith("com.oplus.")) {
            return true;
        }
        if (packageName.startsWith("com.samsung.") || packageName.startsWith("com.sec.android.")) {
            return true;
        }
        if (packageName.startsWith("com.oneplus.")) {
            return true;
        }
        return packageName.startsWith("cn.nubia.") || packageName.startsWith("com.redmagic.");
    }
}
