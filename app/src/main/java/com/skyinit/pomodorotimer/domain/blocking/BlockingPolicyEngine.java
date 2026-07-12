package com.skyinit.pomodorotimer.domain.blocking;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;

import java.util.List;

/**
 * 屏蔽策略纯逻辑引擎：根据 {@link BlockingPolicyConfig} 判定默认白名单、扫描可见性与可屏蔽性。
 * 无 Android 框架依赖，便于单元测试。
 */
public final class BlockingPolicyEngine {

    private final BlockingPolicyConfig config;

    public BlockingPolicyEngine(BlockingPolicyConfig config) {
        this.config = config;
    }

    public boolean isSystemCriticalApp(String packageName) {
        return config.criticalApps.contains(packageName);
    }

    public boolean shouldBeWhitelisted(String packageName) {
        if (isSystemCriticalApp(packageName)) {
            return true;
        }
        if (config.defaultWhitelist.contains(packageName)) {
            return true;
        }
        return matchesAny(packageName, config.whitelistMatchers);
    }

    public boolean shouldBlockByDefault(String packageName) {
        return !shouldBeWhitelisted(packageName) && !isSystemCriticalApp(packageName);
    }

    /**
     * 系统分区应用是否纳入屏蔽管理列表（用户可感知、可主动打开的应用）。
     */
    public boolean shouldIncludeSystemApp(String packageName) {
        if (isSystemCriticalApp(packageName)) {
            return true;
        }
        if (shouldBeWhitelisted(packageName)) {
            return true;
        }
        if (config.scanIncludeExact.contains(packageName)) {
            return true;
        }
        return matchesAny(packageName, config.scanIncludeRules);
    }

    public boolean isUnblockableApp(String packageName) {
        return isSystemCriticalApp(packageName);
    }

    public void applyDefaultPolicy(BlockedApp app) {
        app.isWhitelisted = shouldBeWhitelisted(app.packageName);
        app.isEnabled = shouldBlockByDefault(app.packageName);
    }

    public void applyWhitelistConfig(List<BlockedApp> apps) {
        for (BlockedApp app : apps) {
            if (shouldBeWhitelisted(app.packageName)) {
                app.isWhitelisted = true;
                app.isEnabled = false;
            }
        }
    }

    private boolean matchesAny(String packageName, List<PackageMatchRule> rules) {
        for (PackageMatchRule rule : rules) {
            if (rule.matches(packageName)) {
                return true;
            }
        }
        return false;
    }
}
