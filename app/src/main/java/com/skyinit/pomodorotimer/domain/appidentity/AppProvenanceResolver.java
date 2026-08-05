package com.skyinit.pomodorotimer.domain.appidentity;

/**
 * 根据包名、系统分区标志与是否可启动，解析 {@link AppProvenance}。
 * 无 Android 框架依赖，便于单元测试。
 */
public final class AppProvenanceResolver {

    private final AppIdentityConfig config;
    private final OemPrefixRegistry oemPrefixRegistry;

    public AppProvenanceResolver(AppIdentityConfig config) {
        this.config = config;
        this.oemPrefixRegistry = new OemPrefixRegistry(config);
    }

    /**
     * @param packageName           包名
     * @param isSystemPartitionApp  是否带 {@code ApplicationInfo.FLAG_SYSTEM}
     * @param hasLauncherActivity   是否存在桌面启动入口
     */
    public AppProvenance resolve(String packageName,
                                 boolean isSystemPartitionApp,
                                 boolean hasLauncherActivity) {
        if (packageName == null || packageName.isEmpty()) {
            return AppProvenance.THIRD_PARTY;
        }

        if (config.platformExact.contains(packageName)
                || "android".equals(packageName)) {
            return AppProvenance.PLATFORM;
        }
        if (config.googleExact.contains(packageName)) {
            return AppProvenance.GOOGLE;
        }

        if (oemPrefixRegistry.matchesGooglePrefix(packageName)) {
            return AppProvenance.GOOGLE;
        }
        if (oemPrefixRegistry.matchesPlatformPrefix(packageName)) {
            // Play 商店等可能落在 com.android.* 但已在 googleExact 处理
            return AppProvenance.PLATFORM;
        }

        if (config.oemServiceExact.contains(packageName)) {
            return AppProvenance.OEM_SERVICE;
        }
        if (config.oemPreloadExact.contains(packageName)) {
            return AppProvenance.OEM_PRELOAD;
        }

        if (oemPrefixRegistry.matchesOemPrefix(packageName)) {
            return hasLauncherActivity ? AppProvenance.OEM_PRELOAD : AppProvenance.OEM_SERVICE;
        }

        // 无 OEM/平台前缀：即便在系统分区，也按第三方处理（例如预装但改了包名的应用）
        if (isSystemPartitionApp && !hasLauncherActivity) {
            // 未知系统后台组件：保守视为平台侧服务，避免模糊分类误伤
            return AppProvenance.PLATFORM;
        }

        return AppProvenance.THIRD_PARTY;
    }

    public OemPrefixRegistry getOemPrefixRegistry() {
        return oemPrefixRegistry;
    }
}
