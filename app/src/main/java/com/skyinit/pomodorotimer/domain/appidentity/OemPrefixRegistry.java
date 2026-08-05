package com.skyinit.pomodorotimer.domain.appidentity;

/**
 * OEM / 平台前缀查询，委托 {@link AppIdentityConfig}，作为分类与身份解析的唯一前缀源。
 */
public final class OemPrefixRegistry {

    private final AppIdentityConfig config;

    public OemPrefixRegistry(AppIdentityConfig config) {
        this.config = config;
    }

    public boolean matchesPlatformPrefix(String packageName) {
        return startsWithAny(packageName, config.platformPrefixes);
    }

    public boolean matchesGooglePrefix(String packageName) {
        return startsWithAny(packageName, config.googlePrefixes);
    }

    public boolean matchesOemPrefix(String packageName) {
        return startsWithAny(packageName, config.oemPrefixes);
    }

    private static boolean startsWithAny(String packageName, java.util.List<String> prefixes) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isEmpty() && packageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
