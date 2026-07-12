package com.skyinit.pomodorotimer.domain.blocking;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class BlockingPolicyEngineTest {

    private BlockingPolicyEngine engine;

    @Before
    public void setUp() {
        Context context = org.robolectric.RuntimeEnvironment.getApplication();
        BlockingPolicyRulesLoader.init(context);
        engine = BlockingPolicyRulesLoader.getInstance().createEngine();
    }

    @Test
    public void systemCriticalApps_areNeverBlocked() {
        assertTrue(engine.isSystemCriticalApp("com.android.systemui"));
        assertTrue(engine.isSystemCriticalApp("com.android.settings"));
        assertFalse(engine.shouldBlockByDefault("com.android.systemui"));
        assertTrue(engine.shouldBeWhitelisted("com.android.systemui"));
        assertTrue(engine.isUnblockableApp("com.android.systemui"));
    }

    @Test
    public void defaultWhitelistApps_areNotBlockedByDefault() {
        assertTrue(engine.shouldBeWhitelisted("com.android.dialer"));
        assertFalse(engine.shouldBlockByDefault("com.android.dialer"));
    }

    @Test
    public void googleMessages_isSystemMessaging_notBlockedByDefault() {
        assertTrue(engine.shouldBeWhitelisted("com.google.android.apps.messaging"));
        assertFalse(engine.shouldBlockByDefault("com.google.android.apps.messaging"));
    }

    @Test
    public void typicalThirdPartyApps_areBlockedByDefault() {
        assertFalse(engine.shouldBeWhitelisted("com.eg.android.AlipayGphone"));
        assertTrue(engine.shouldBlockByDefault("com.eg.android.AlipayGphone"));
        assertFalse(engine.shouldBeWhitelisted("com.tencent.mm"));
        assertTrue(engine.shouldBlockByDefault("com.tencent.mm"));
    }

    @Test
    public void healthApps_matchWhitelistMatchers() {
        assertTrue(engine.shouldBeWhitelisted("com.huawei.health"));
        assertFalse(engine.shouldBlockByDefault("com.hihonor.health"));
    }

    @Test
    public void brandSystemApps_areIncludedInScan() {
        assertTrue(engine.shouldIncludeSystemApp("com.xiaomi.market"));
        assertTrue(engine.shouldBlockByDefault("com.xiaomi.market"));
    }

    @Test
    public void calculator_isWhitelistedByDefault() {
        assertTrue(engine.shouldBeWhitelisted("com.android.calculator2"));
        assertFalse(engine.shouldBlockByDefault("com.miui.calculator"));
    }

    @Test
    public void commonSocialApps_blockByDefault() {
        assertTrue(engine.shouldBlockByDefault("com.tencent.mobileqq"));
        assertTrue(engine.shouldBlockByDefault("com.sina.weibo"));
    }

    @Test
    public void adAndEntertainmentPackages_blockByDefault() {
        assertTrue(engine.shouldBlockByDefault("com.miui.systemAdSolution"));
        assertTrue(engine.shouldBlockByDefault("com.xiaomi.market"));
        assertTrue(engine.shouldBlockByDefault("com.sina.weibo"));
        assertTrue(engine.shouldBlockByDefault("com.alibaba.android.rimet"));
    }

    @Test
    public void realmeAndNothingVendorPackages_includedInScan() {
        assertTrue(engine.shouldIncludeSystemApp("com.realme.browser"));
        assertTrue(engine.shouldIncludeSystemApp("com.nothing.launcher"));
    }
}
