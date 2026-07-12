package com.skyinit.pomodorotimer.domain.blocking;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 可序列化的包名匹配规则：精确包名 + 前缀 + 子串，支持排除列表。
 */
public final class PackageMatchRule {

    public final Set<String> exactPackages;
    public final List<String> packagePrefixes;
    public final List<String> packageContains;
    public final Set<String> excludeExactPackages;

    public PackageMatchRule(Set<String> exactPackages,
                            List<String> packagePrefixes,
                            List<String> packageContains,
                            Set<String> excludeExactPackages) {
        this.exactPackages = exactPackages != null ? exactPackages : Collections.emptySet();
        this.packagePrefixes = packagePrefixes != null ? packagePrefixes : Collections.emptyList();
        this.packageContains = packageContains != null ? packageContains : Collections.emptyList();
        this.excludeExactPackages = excludeExactPackages != null
                ? excludeExactPackages
                : Collections.emptySet();
    }

    public boolean matches(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        if (excludeExactPackages.contains(packageName)) {
            return false;
        }
        if (exactPackages.contains(packageName)) {
            return true;
        }
        for (String prefix : packagePrefixes) {
            if (packageName.startsWith(prefix)) {
                return true;
            }
        }
        for (String keyword : packageContains) {
            if (packageName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static PackageMatchRule fromExactPackages(Set<String> packages) {
        return new PackageMatchRule(packages, Collections.emptyList(), Collections.emptyList(), null);
    }

    public PackageMatchRule mergeExactPackages(Set<String> extra) {
        if (extra == null || extra.isEmpty()) {
            return this;
        }
        Set<String> merged = new HashSet<>(exactPackages);
        merged.addAll(extra);
        return new PackageMatchRule(merged, packagePrefixes, packageContains, excludeExactPackages);
    }
}
