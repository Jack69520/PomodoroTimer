package com.skyinit.pomodorotimer.util;

import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.skyinit.pomodorotimer.domain.appidentity.AppProvenance;
import com.skyinit.pomodorotimer.domain.appidentity.AppProvenanceResolver;
import com.skyinit.pomodorotimer.domain.appidentity.AppIdentityRulesLoader;

/**
 * 应用分类器：精确包名 HashSet 查找 + JSON 规则 + 来源回落 + 系统 category 兜底。
 * 分类逻辑与屏蔽策略解耦；「系统服务」仅收纳无法功能归类的平台/厂商服务组件。
 */
public final class AppCategoryClassifier {

    private AppCategoryClassifier() {
    }

    public static String classify(String packageName, String appName) {
        return classify(packageName, appName, null, null);
    }

    public static String classify(String packageName, String appName, ApplicationInfo appInfo) {
        return classify(packageName, appName, appInfo, null);
    }

    public static String classify(String packageName,
                                  String appName,
                                  ApplicationInfo appInfo,
                                  AppProvenance provenance) {
        if (packageName == null || packageName.isEmpty()) {
            return AppCategory.OTHER;
        }
        String safeAppName = appName != null ? appName : packageName;
        AppCategoryRulesLoader loader = AppCategoryRulesLoader.getInstance();

        String exactCategory = loader.getCategoryByExactPackage(packageName);
        if (exactCategory != null) {
            return exactCategory;
        }

        AppProvenance resolved = provenance != null
                ? provenance
                : resolveProvenanceFallback(packageName, appInfo);
        boolean skipFuzzy = resolved.skipsFuzzyCategoryRules();

        for (AppCategoryRulesLoader.CategoryRule rule : loader.getRulesInOrder()) {
            if (AppCategory.SYSTEM.equals(rule.category)) {
                continue;
            }
            if (rule.matches(packageName, safeAppName, skipFuzzy)) {
                return rule.category;
            }
        }

        if (!skipFuzzy && packageName.contains("work") && !isSystemWorkPackage(packageName)) {
            return AppCategory.JOB;
        }

        // 仅平台 / Google / 厂商服务回落到「系统服务」；OEM 预装与第三方走其他
        if (resolved == AppProvenance.PLATFORM
                || resolved == AppProvenance.GOOGLE
                || resolved == AppProvenance.OEM_SERVICE) {
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

    private static AppProvenance resolveProvenanceFallback(String packageName,
                                                           ApplicationInfo appInfo) {
        try {
            AppProvenanceResolver resolver =
                    AppIdentityRulesLoader.getInstance().createResolver();
            boolean system = appInfo != null
                    && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            // 无 Context 时无法查 launcher；OEM 前缀默认按预装处理，避免误入系统服务桶
            boolean hasLauncher = true;
            return resolver.resolve(packageName, system, hasLauncher);
        } catch (IllegalStateException e) {
            return AppProvenance.THIRD_PARTY;
        }
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
}
