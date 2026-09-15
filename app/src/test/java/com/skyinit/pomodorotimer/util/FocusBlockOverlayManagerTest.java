package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.provider.Settings;

import com.skyinit.pomodorotimer.TestApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link FocusBlockOverlayManager} 单元测试。
 * <p>
 * 遮罩展示强依赖 WindowManager，完整 UI 更适合仪器测试；
 * 此处只覆盖 {@link FocusBlockOverlayManager#canShowOverlay()} 权限门闩，
 * 确保无悬浮窗权限时不会尝试 addView。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class FocusBlockOverlayManagerTest {

    private Context context;
    private FocusBlockOverlayManager manager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        manager = new FocusBlockOverlayManager(context);
    }

    /** 默认 Robolectric 环境通常未授予悬浮窗；无权限时 canShow 应为 false。 */
    @Test
    public void canShowOverlay_false_withoutPermission() {
        ShadowSettings.setCanDrawOverlays(false);
        assertFalse(Settings.canDrawOverlays(context));
        assertFalse(manager.canShowOverlay());
    }

    /** 授予悬浮窗后 canShow 应为 true。 */
    @Test
    public void canShowOverlay_true_withPermission() {
        ShadowSettings.setCanDrawOverlays(true);
        assertTrue(Settings.canDrawOverlays(context));
        assertTrue(manager.canShowOverlay());
    }

    /**
     * 无权限时调用 show 不得崩溃（内部直接 return），
     * 用于回归「权限被撤后仍收到拦截事件」路径。
     */
    @Test
    public void show_withoutPermission_doesNotThrow() {
        ShadowSettings.setCanDrawOverlays(false);
        manager.show("测试应用");
        manager.hide();
    }
}
