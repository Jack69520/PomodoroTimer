package com.skyinit.pomodorotimer.domain.appidentity;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 应用来源身份规则的不可变快照。
 */
public final class AppIdentityConfig {

    public final int version;
    public final Set<String> platformExact;
    public final Set<String> googleExact;
    public final List<String> platformPrefixes;
    public final List<String> googlePrefixes;
    public final List<String> oemPrefixes;
    public final Set<String> oemServiceExact;
    public final Set<String> oemPreloadExact;

    public AppIdentityConfig(int version,
                             Set<String> platformExact,
                             Set<String> googleExact,
                             List<String> platformPrefixes,
                             List<String> googlePrefixes,
                             List<String> oemPrefixes,
                             Set<String> oemServiceExact,
                             Set<String> oemPreloadExact) {
        this.version = version;
        this.platformExact = platformExact != null ? platformExact : Collections.emptySet();
        this.googleExact = googleExact != null ? googleExact : Collections.emptySet();
        this.platformPrefixes = platformPrefixes != null ? platformPrefixes : Collections.emptyList();
        this.googlePrefixes = googlePrefixes != null ? googlePrefixes : Collections.emptyList();
        this.oemPrefixes = oemPrefixes != null ? oemPrefixes : Collections.emptyList();
        this.oemServiceExact = oemServiceExact != null ? oemServiceExact : Collections.emptySet();
        this.oemPreloadExact = oemPreloadExact != null ? oemPreloadExact : Collections.emptySet();
    }
}
