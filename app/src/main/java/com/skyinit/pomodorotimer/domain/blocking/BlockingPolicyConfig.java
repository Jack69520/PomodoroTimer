package com.skyinit.pomodorotimer.domain.blocking;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 屏蔽策略配置的不可变快照，由 {@link BlockingPolicyRulesLoader} 从 JSON 构建。
 */
public final class BlockingPolicyConfig {

    public final int version;
    public final Set<String> criticalApps;
    public final Set<String> defaultWhitelist;
    public final Set<String> scanIncludeExact;
    public final List<PackageMatchRule> scanIncludeRules;
    public final List<PackageMatchRule> whitelistMatchers;
    public final List<AppTypeRule> appTypeRules;

    public BlockingPolicyConfig(int version,
                                Set<String> criticalApps,
                                Set<String> defaultWhitelist,
                                Set<String> scanIncludeExact,
                                List<PackageMatchRule> scanIncludeRules,
                                List<PackageMatchRule> whitelistMatchers,
                                List<AppTypeRule> appTypeRules) {
        this.version = version;
        this.criticalApps = criticalApps != null ? criticalApps : Collections.emptySet();
        this.defaultWhitelist = defaultWhitelist != null ? defaultWhitelist : Collections.emptySet();
        this.scanIncludeExact = scanIncludeExact != null ? scanIncludeExact : Collections.emptySet();
        this.scanIncludeRules = scanIncludeRules != null ? scanIncludeRules : Collections.emptyList();
        this.whitelistMatchers = whitelistMatchers != null ? whitelistMatchers : Collections.emptyList();
        this.appTypeRules = appTypeRules != null ? appTypeRules : Collections.emptyList();
    }

    public static final class AppTypeRule {
        public final String labelKey;
        private final PackageMatchRule matcher;

        public AppTypeRule(String labelKey,
                           Set<String> exactPackages,
                           List<String> packagePrefixes,
                           List<String> packageContains,
                           Set<String> excludeExactPackages) {
            this.labelKey = labelKey;
            this.matcher = new PackageMatchRule(
                    exactPackages, packagePrefixes, packageContains, excludeExactPackages);
        }

        public boolean matches(String packageName) {
            return matcher.matches(packageName);
        }
    }
}
