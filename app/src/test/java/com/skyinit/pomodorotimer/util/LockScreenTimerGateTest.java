package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link LockScreenTimerGate} 单元测试。
 * <p>
 * 覆盖锁屏全屏计时的「门槛判定」：会话是否可展示、开启前置条件、
 * 降级策略，以及跳转系统设置页的 Intent 构造。
 * 该类无状态、无副作用（除读权限外），适合作为锁屏模块的稳定回归基线。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LockScreenTimerGateTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }

    // ---------- 会话资格：计时运行中或暂停中才允许锁屏展示 ----------

    /** 运行中：应视为有资格展示锁屏计时。 */
    @Test
    public void sessionEligible_whenRunning() {
        assertTrue(LockScreenTimerGate.isSessionEligible(true, false));
    }

    /** 暂停中：仍视为有资格（用户未结束会话，锁屏应可拉回）。 */
    @Test
    public void sessionEligible_whenPaused() {
        assertTrue(LockScreenTimerGate.isSessionEligible(false, true));
    }

    /** 既未运行也未暂停（空闲）：不应展示锁屏计时。 */
    @Test
    public void sessionNotEligible_whenIdle() {
        assertFalse(LockScreenTimerGate.isSessionEligible(false, false));
    }

    /** 运行且暂停同时为 true 时仍合格（防御性：调用方状态短暂不一致）。 */
    @Test
    public void sessionEligible_whenBothFlagsTrue() {
        assertTrue(LockScreenTimerGate.isSessionEligible(true, true));
    }

    // ---------- 开启前置条件：OK / 缺通知 / 缺 FSI / 两者皆缺 ----------

    /**
     * API 28 上通知权限默认视为已授予，FSI 也视为可用，
     * 因此 evaluate 应返回 {@link LockScreenTimerGate.EnablePrecondition#OK}。
     */
    @Test
    public void evaluatePreconditions_onApi28_isOk() {
        assertEquals(
                LockScreenTimerGate.EnablePrecondition.OK,
                LockScreenTimerGate.evaluateEnablePreconditions(context));
    }

    /** 全部就绪时可开启（含非降级完整路径）。 */
    @Test
    public void canEnableWithDegradedMode_allowsOk() {
        assertTrue(LockScreenTimerGate.canEnableWithDegradedMode(
                LockScreenTimerGate.EnablePrecondition.OK));
    }

    /**
     * 仅缺全屏 Intent：产品允许「降级开启」（先开偏好，后续补授权）。
     * 这是混合亮屏策略的重要分支，勿轻易改严。
     */
    @Test
    public void canEnableWithDegradedMode_allowsMissingFsiOnly() {
        assertTrue(LockScreenTimerGate.canEnableWithDegradedMode(
                LockScreenTimerGate.EnablePrecondition.NEED_FULL_SCREEN_INTENT));
    }

    /** 缺通知权限时禁止开启（通知是拉起全屏 Intent 的基础）。 */
    @Test
    public void canEnableWithDegradedMode_rejectsMissingNotification() {
        assertFalse(LockScreenTimerGate.canEnableWithDegradedMode(
                LockScreenTimerGate.EnablePrecondition.NEED_NOTIFICATION));
        assertFalse(LockScreenTimerGate.canEnableWithDegradedMode(
                LockScreenTimerGate.EnablePrecondition.NEED_BOTH));
    }

    /** mustHaveNotification：缺通知或两者皆缺时为 true。 */
    @Test
    public void mustHaveNotification_forNotifRelatedPreconditions() {
        assertTrue(LockScreenTimerGate.mustHaveNotification(
                LockScreenTimerGate.EnablePrecondition.NEED_NOTIFICATION));
        assertTrue(LockScreenTimerGate.mustHaveNotification(
                LockScreenTimerGate.EnablePrecondition.NEED_BOTH));
        assertFalse(LockScreenTimerGate.mustHaveNotification(
                LockScreenTimerGate.EnablePrecondition.OK));
        assertFalse(LockScreenTimerGate.mustHaveNotification(
                LockScreenTimerGate.EnablePrecondition.NEED_FULL_SCREEN_INTENT));
    }

    // ---------- 系统设置跳转 Intent ----------

    /**
     * API 28（&lt; O 实际走 O 分支因 minSdk=28）：通知设置 Intent 应带上本包名。
     */
    @Test
    public void createNotificationSettingsIntent_targetsThisPackage() {
        Intent intent = LockScreenTimerGate.createNotificationSettingsIntent(context);
        assertNotNull(intent);
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.getAction());
        assertEquals(context.getPackageName(),
                intent.getStringExtra(Settings.EXTRA_APP_PACKAGE));
    }

    /**
     * API 28 低于 UPSIDE_DOWN_CAKE：FSI 设置页回退到应用详情，
     * 避免在低版本构造无法解析的 Action。
     */
    @Test
    public void createFullScreenIntentSettings_onApi28_fallsBackToAppDetails() {
        Intent intent = LockScreenTimerGate.createFullScreenIntentSettingsIntent(context);
        assertNotNull(intent);
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.getAction());
        assertNotNull(intent.getData());
        assertTrue(intent.getData().toString().contains(context.getPackageName()));
    }

    /**
     * API 34：应使用系统「管理全屏 Intent」专用设置页。
     */
    @Test
    @Config(sdk = 34)
    public void createFullScreenIntentSettings_onApi34_usesManageFsiAction() {
        Intent intent = LockScreenTimerGate.createFullScreenIntentSettingsIntent(context);
        assertEquals(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, intent.getAction());
        assertNotNull(intent.getData());
        assertTrue(intent.getData().toString().contains(context.getPackageName()));
    }

    /**
     * API 34 且未授予 POST_NOTIFICATIONS 时，应至少需要通知权限
     *（FSI 是否同时缺失取决于 ShadowNotificationManager，故只断言「非 OK」且 mustHaveNotification）。
     */
    @Test
    @Config(sdk = 34)
    public void evaluatePreconditions_onApi34_withoutPostNotifications_needsNotification() {
        LockScreenTimerGate.EnablePrecondition precondition =
                LockScreenTimerGate.evaluateEnablePreconditions(context);
        // Robolectric 默认未授予 POST_NOTIFICATIONS
        assertTrue(
                "API34 默认无通知权限时应要求补通知",
                LockScreenTimerGate.mustHaveNotification(precondition));
        assertFalse(LockScreenTimerGate.canEnableWithDegradedMode(precondition));
    }
}
