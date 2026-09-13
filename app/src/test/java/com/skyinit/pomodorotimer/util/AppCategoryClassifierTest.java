package com.skyinit.pomodorotimer.util;

import android.content.Context;

import com.skyinit.pomodorotimer.domain.appidentity.AppIdentityRulesLoader;
import com.skyinit.pomodorotimer.domain.appidentity.AppProvenance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class AppCategoryClassifierTest {

    @Before
    public void setUp() {
        Context context = org.robolectric.RuntimeEnvironment.getApplication();
        AppCategory.init(context);
        AppIdentityRulesLoader.init(context);
        AppCategoryRulesLoader.init(context);
    }

    @Test
    public void vendorAppMarkets_areTools() {
        assertEquals(AppCategory.TOOL, AppCategoryClassifier.classify(
                "com.huawei.appmarket", "华为应用市场", null, AppProvenance.OEM_PRELOAD));
        assertEquals(AppCategory.TOOL, AppCategoryClassifier.classify(
                "com.hihonor.appmarket", "荣耀应用市场", null, AppProvenance.OEM_PRELOAD));
        assertEquals(AppCategory.TOOL, AppCategoryClassifier.classify(
                "com.xiaomi.market", "应用商店", null, AppProvenance.OEM_PRELOAD));
        assertEquals(AppCategory.TOOL, AppCategoryClassifier.classify(
                "com.vivo.appstore", "应用商店", null, AppProvenance.OEM_PRELOAD));
        assertEquals(AppCategory.TOOL, AppCategoryClassifier.classify(
                "com.heytap.market", "软件商店", null, AppProvenance.OEM_PRELOAD));
        assertEquals(AppCategory.TOOL, AppCategoryClassifier.classify(
                "com.sec.android.app.samsungapps", "Galaxy Store", null, AppProvenance.OEM_PRELOAD));
        assertEquals(AppCategory.TOOL, AppCategoryClassifier.classify(
                "com.android.vending", "Play 商店", null, AppProvenance.GOOGLE));
    }

    @Test
    public void vivoBrowserPreload_canBeToolOrOther_notForcedSystemService() {
        String category = AppCategoryClassifier.classify(
                "com.vivo.browser", "浏览器", null, AppProvenance.OEM_PRELOAD);
        // 浏览器关键词 / 模糊规则应命中实用工具或其它功能类，而非系统服务桶
        assertEquals(false, AppCategory.SYSTEM.equals(category));
    }

    @Test
    public void miuiVideo_exactEntertainment() {
        String category = AppCategoryClassifier.classify(
                "com.miui.video", "小米视频", null, AppProvenance.OEM_PRELOAD);
        assertEquals(AppCategory.ENTERTAINMENT, category);
    }

    @Test
    public void oemService_fallsBackToSystemService() {
        String category = AppCategoryClassifier.classify(
                "com.huawei.hwid", "华为账号", null, AppProvenance.OEM_SERVICE);
        assertEquals(AppCategory.SYSTEM, category);
    }

    @Test
    public void platformUnknown_fallsBackToSystemService() {
        String category = AppCategoryClassifier.classify(
                "com.android.unknown.service", "Unknown", null, AppProvenance.PLATFORM);
        assertEquals(AppCategory.SYSTEM, category);
    }
}
