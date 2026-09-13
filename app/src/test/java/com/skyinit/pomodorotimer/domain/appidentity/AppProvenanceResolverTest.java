package com.skyinit.pomodorotimer.domain.appidentity;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class AppProvenanceResolverTest {

    private AppProvenanceResolver resolver;

    @Before
    public void setUp() {
        Context context = org.robolectric.RuntimeEnvironment.getApplication();
        AppIdentityRulesLoader.init(context);
        resolver = AppIdentityRulesLoader.getInstance().createResolver();
    }

    @Test
    public void platformExact_isPlatform() {
        assertEquals(AppProvenance.PLATFORM,
                resolver.resolve("com.android.systemui", true, false));
    }

    @Test
    public void googleExact_isGoogle() {
        assertEquals(AppProvenance.GOOGLE,
                resolver.resolve("com.google.android.gsf", true, false));
        assertEquals(AppProvenance.GOOGLE,
                resolver.resolve("com.android.vending", true, true));
    }

    @Test
    public void oemWithLauncher_isPreload() {
        assertEquals(AppProvenance.OEM_PRELOAD,
                resolver.resolve("com.vivo.browser", true, true));
    }

    @Test
    public void oemWithoutLauncher_isService() {
        assertEquals(AppProvenance.OEM_SERVICE,
                resolver.resolve("com.miui.system", true, false));
    }

    @Test
    public void oemServiceExact_overridesLauncher() {
        assertEquals(AppProvenance.OEM_SERVICE,
                resolver.resolve("com.huawei.hwid", true, true));
    }

    @Test
    public void thirdParty_isThirdParty() {
        assertEquals(AppProvenance.THIRD_PARTY,
                resolver.resolve("com.tencent.mm", false, true));
    }

    @Test
    public void skipsFuzzy_onlyForPlatformGoogleOemService() {
        assertTrue(AppProvenance.PLATFORM.skipsFuzzyCategoryRules());
        assertTrue(AppProvenance.GOOGLE.skipsFuzzyCategoryRules());
        assertTrue(AppProvenance.OEM_SERVICE.skipsFuzzyCategoryRules());
        assertFalse(AppProvenance.OEM_PRELOAD.skipsFuzzyCategoryRules());
        assertFalse(AppProvenance.THIRD_PARTY.skipsFuzzyCategoryRules());
    }
}
