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

    public BlockingRole resolveRole(String packageName) {
        if (isCritical(packageName)) {
            return BlockingRole.CRITICAL;
        }
        if (shouldBeWhitelisted(packageName)) {
            return BlockingRole.DEFAULT_ALLOW;
        }
        return BlockingRole.DEFAULT_BLOCK;
    }

    public boolean isCritical(String packageName) {
        return config.criticalApps.contains(packageName);
    }

    /** @deprecated 使用 {@link #isCritical(String)} */
    @Deprecated
    public boolean isSystemCriticalApp(String packageName) {
        return isCritical(packageName);
    }

    public boolean shouldBeWhitelisted(String packageName) {
        if (isCritical(packageName)) {
            return true;
        }
        if (config.defaultWhitelist.contains(packageName)) {
            return true;
        }
        return matchesAny(packageName, config.whitelistMatchers);
    }

    public boolean shouldBlockByDefault(String packageName) {
        return resolveRole(packageName) == BlockingRole.DEFAULT_BLOCK;
    }

    /**
     * 系统分区应用是否纳入屏蔽管理列表（用户可感知、可主动打开的应用）。
     */
    public boolean shouldIncludeInManagedList(String packageName) {
        if (isCritical(packageName)) {
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

    /** @deprecated 使用 {@link #shouldIncludeInManagedList(String)} */
    @Deprecated
    public boolean shouldIncludeSystemApp(String packageName) {
        return shouldIncludeInManagedList(packageName);
    }

    public boolean isUnblockableApp(String packageName) {
        return isCritical(packageName);
    }

    public void applyDefaultPolicy(BlockedApp app) {
        BlockingRole role = resolveRole(app.packageName);
        app.blockingRole = role.toStorage();
        app.isWhitelisted = role != BlockingRole.DEFAULT_BLOCK;
        app.isEnabled = role == BlockingRole.DEFAULT_BLOCK;
    }

    public void applyWhitelistConfig(List<BlockedApp> apps) {
        for (BlockedApp app : apps) {
            if (shouldBeWhitelisted(app.packageName)) {
                app.isWhitelisted = true;
                app.isEnabled = false;
                if (app.blockingRole == null || app.blockingRole.isEmpty()) {
                    app.blockingRole = resolveRole(app.packageName).toStorage();
                }
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
