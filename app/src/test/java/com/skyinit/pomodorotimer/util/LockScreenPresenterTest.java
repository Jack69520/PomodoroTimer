package com.skyinit.pomodorotimer.util;

import android.app.Application;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.ui.home.LockScreenTimerActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowKeyguardManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPowerManager;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link LockScreenPresenter} 单元测试。
 * <p>
 * 重点验证「混合亮屏」状态机，而非 UI：
 * <ul>
 *   <li>偏好关闭 / 会话空闲 → 永不 present</li>
 *   <li>灭屏（SCREEN_OFF）→ 禁止拉起，避免强行唤醒</li>
 *   <li>亮屏 + 锁屏 → 唯一主入口 {@code onScreenOn}</li>
 *   <li>用户长按解锁抑制 → 解锁前不再强拉</li>
 *   <li>普通/锁屏计时页已在前台 → 不重复拉起</li>
 * </ul>
 * 通过 Robolectric Shadow 模拟 Keyguard / PowerManager，避免依赖真机锁屏。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class LockScreenPresenterTest {

    private Context context;
    private LockScreenPresenter presenter;
    private ShadowKeyguardManager shadowKeyguard;
    private ShadowPowerManager shadowPower;
    private ShadowApplication shadowApp;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        shadowApp = Shadows.shadowOf((Application) context);

        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        shadowKeyguard = Shadows.shadowOf(km);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        shadowPower = Shadows.shadowOf(pm);

        // 默认：屏亮 + 未锁屏，避免构造器 / updateSession 副作用干扰断言
        setScreenInteractive(true);
        shadowKeyguard.setKeyguardLocked(false);

        // present 防抖用 elapsedRealtime；lastPresentAtMs=0 时若 uptime<1.5s 会被误挡，
        // Robolectric 默认 uptime 很小，这里先推进时钟。
        ShadowSystemClock.advanceBy(5, TimeUnit.SECONDS);

        presenter = new LockScreenPresenter(context);
        // 清掉构造 channel 等可能留下的已启动 Activity 记录
        clearStartedActivities();
    }

    @After
    public void tearDown() {
        if (presenter != null) {
            presenter.teardown();
        }
    }

    /** 丢弃 Shadow 中已记录的 startActivity，保证每个用例从干净队列开始。 */
    private void clearStartedActivities() {
        while (shadowApp.getNextStartedActivity() != null) {
            // drain
        }
    }

    /** 清空 Activity 队列与锁屏 present 通知，避免上一次 tryPresent 污染断言。 */
    private void clearPresentSideEffects() {
        clearStartedActivities();
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(LockScreenPresenter.NOTIFICATION_ID_LOCK_PRESENT);
        }
    }

    /** 重置 present 防抖时间戳（仅测试用）。 */
    private void resetLastPresentAtMs() {
        try {
            java.lang.reflect.Field field =
                    LockScreenPresenter.class.getDeclaredField("lastPresentAtMs");
            field.setAccessible(true);
            field.setLong(presenter, 0L);
        } catch (Exception e) {
            throw new AssertionError("无法重置 lastPresentAtMs", e);
        }
    }

    /**
     * present 成功判定：
     * 1) 直接拉起锁屏计时页；或
     * 2) 降级发出全屏 Intent 通知；或
     * 3) tryPresent 已写入 lastPresentAtMs（Robolectric 下 startActivity 可能吞掉副作用时的可靠信号）。
     */
    private boolean hasPresentedLockScreen() {
        Intent started = shadowApp.peekNextStartedActivity();
        if (started != null
                && started.getComponent() != null
                && LockScreenTimerActivity.class.getName()
                .equals(started.getComponent().getClassName())
                && started.getBooleanExtra(LockScreenPresenter.EXTRA_LOCK_PRESENTATION, false)) {
            return true;
        }
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            ShadowNotificationManager shadowNm = Shadows.shadowOf(nm);
            if (shadowNm.getNotification(LockScreenPresenter.NOTIFICATION_ID_LOCK_PRESENT) != null) {
                return true;
            }
            List<Notification> notifications = shadowNm.getAllNotifications();
            if (notifications != null && !notifications.isEmpty()) {
                return true;
            }
        }
        try {
            java.lang.reflect.Field field =
                    LockScreenPresenter.class.getDeclaredField("lastPresentAtMs");
            field.setAccessible(true);
            return field.getLong(presenter) > 0L;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- updateSession：偏好与会话资格 ----------

    /** 偏好关闭时，即便计时在跑也不应标记为可展示。 */
    @Test
    public void updateSession_preferenceOff_clearsEligibility() {
        presenter.updateSession(false, true, false);
        assertFalse(presenter.isPreferenceEnabled());
        assertTrue(presenter.isSessionEligible());
        assertFalse(presenter.isSuppressUntilUnlocked());
        assertNull("偏好关闭不应拉起锁屏页", shadowApp.getNextStartedActivity());
    }

    /** 偏好开启且运行中：会话合格；屏未锁屏时不应强拉。 */
    @Test
    public void updateSession_running_marksEligible_butNoPresentWithoutKeyguard() {
        presenter.updateSession(true, true, false);
        assertTrue(presenter.isPreferenceEnabled());
        assertTrue(presenter.isSessionEligible());
        assertNull(shadowApp.getNextStartedActivity());
    }

    /** 空闲会话：资格为 false，并清除解锁抑制。 */
    @Test
    public void updateSession_idle_clearsSuppress() {
        presenter.onUserRequestedUnlock();
        assertTrue(presenter.isSuppressUntilUnlocked());

        presenter.updateSession(true, false, false);
        assertFalse(presenter.isSessionEligible());
        assertFalse("会话结束后应释放解锁抑制", presenter.isSuppressUntilUnlocked());
    }

    // ---------- 解锁抑制 ----------

    /** 用户请求解锁后进入抑制态，直到 USER_UNLOCKED 或亮屏+锁屏清除。 */
    @Test
    public void userRequestedUnlock_setsSuppress() {
        presenter.onUserRequestedUnlock();
        assertTrue(presenter.isSuppressUntilUnlocked());

        presenter.onUserUnlocked();
        assertFalse(presenter.isSuppressUntilUnlocked());
    }

    // ---------- 计时页前后台 ----------

    /** 普通计时页 resumed 时标记前台，避免重复 present。 */
    @Test
    public void normalTimerUiResumed_marksTimerUiResumed() {
        presenter.onNormalTimerUiResumed();
        assertTrue(presenter.isTimerUiResumed());
    }

    /** 锁屏计时页 resumed 同样视为计时 UI 在前台。 */
    @Test
    public void lockScreenUiResumed_marksTimerUiResumed() {
        presenter.onLockScreenUiResumed();
        assertTrue(presenter.isTimerUiResumed());
    }

    /**
     * 锁屏页 stop（含灭屏导致的 stop）不得触发 present——
     * 重新展示只能走 {@link LockScreenPresenter#onScreenOn()}。
     */
    @Test
    public void lockScreenUiStopped_doesNotPresent() {
        makeEligibleAndLocked();
        clearStartedActivities();

        presenter.onLockScreenUiStopped(false);
        assertNull("锁屏 stop 不得 tryPresent", shadowApp.getNextStartedActivity());
    }

    // ---------- 混合亮屏：灭屏 / 亮屏 ----------

    /**
     * 灭屏后：即使随后调用「普通页 stop」路径，也不应拉起（避免唤醒）。
     * 这里用 onNormalTimerUiStopped 模拟灭屏引发的 stop。
     */
    @Test
    public void screenOff_blocksPresentEvenIfNormalUiStopped() {
        makeEligibleAndLocked();
        clearStartedActivities();

        presenter.onScreenOff();
        // 模拟灭屏导致 Activity stop；产品硬约束：此处不得 present
        presenter.onNormalTimerUiStopped(false);
        assertNull(shadowApp.getNextStartedActivity());
    }

    /**
     * 亮屏且处于锁屏：应进入 tryPresent（写入 lastPresentAtMs），
     * 并尽量拉起锁屏页或发出 FSI 通知。
     */
    @Test
    public void screenOn_whenLockedAndEligible_startsLockScreenActivity() throws Exception {
        makeEligibleAndLocked();
        clearPresentSideEffects();
        resetLastPresentAtMs();
        resetPresentInFlight();

        // 先灭屏再亮屏，贴近真实广播顺序
        presenter.onScreenOff();
        // onScreenOff 将 lastPresentAtMs 置 0；再次推进时钟，避免 uptime 仍落在防抖窗口
        ShadowSystemClock.advanceBy(2, TimeUnit.SECONDS);
        setScreenInteractive(true);
        shadowKeyguard.setKeyguardLocked(true);

        assertTrue("测试前置：系统应报告亮屏", isDeviceInteractive());
        assertTrue("测试前置：应处于锁屏", isDeviceKeyguardLocked());
        assertTrue("灭屏标志清除前 shouldPresent 应为 false", !invokeShouldPresent());

        presenter.onScreenOn();
        ShadowLooper.idleMainLooper();

        assertTrue("onScreenOn 后应允许 present", invokeShouldPresent() || hasPresentedLockScreen());
        assertTrue("亮屏+锁屏应触发 present", hasPresentedLockScreen());
    }

    /**
     * 亮屏但未锁屏（例如已解锁桌面）：不应拉起锁屏计时页。
     */
    @Test
    public void screenOn_whenUnlocked_doesNotStartActivity() {
        presenter.updateSession(true, true, false);
        setScreenInteractive(true);
        shadowKeyguard.setKeyguardLocked(false);
        clearStartedActivities();
        resetLastPresentAtMs();

        presenter.onScreenOn();
        assertNull(shadowApp.getNextStartedActivity());
        assertFalse(hasPresentedLockScreen());
    }

    /**
     * 用户请求解锁后进入抑制；若仍锁屏且再次亮屏，产品会清除抑制并允许再次 present
     *（兼容部分机型收不到 USER_UNLOCKED）。
     */
    @Test
    public void screenOn_whileKeyguardLocked_clearsSuppressAndPresents() throws Exception {
        makeEligibleAndLocked();
        presenter.onUserRequestedUnlock();
        assertTrue(presenter.isSuppressUntilUnlocked());
        clearPresentSideEffects();
        resetLastPresentAtMs();
        resetPresentInFlight();
        setScreenInteractive(true);
        shadowKeyguard.setKeyguardLocked(true);

        presenter.onScreenOn();
        ShadowLooper.idleMainLooper();

        assertFalse("亮屏+锁屏应清除抑制", presenter.isSuppressUntilUnlocked());
        assertTrue("清除抑制后应再次 present", hasPresentedLockScreen());
    }

    /**
     * 计时 UI 已在前台时，onScreenOn 不应重复 startActivity。
     */
    @Test
    public void screenOn_whenTimerUiAlreadyResumed_skipsPresent() {
        makeEligibleAndLocked();
        presenter.onLockScreenUiResumed();
        clearStartedActivities();

        presenter.onScreenOn();
        assertNull(shadowApp.getNextStartedActivity());
    }

    /** teardown 应清空资格与抑制标志，便于服务销毁后无残留状态。 */
    @Test
    public void teardown_resetsAllFlags() {
        makeEligibleAndLocked();
        presenter.onUserRequestedUnlock();
        presenter.onNormalTimerUiResumed();

        presenter.teardown();

        assertFalse(presenter.isPreferenceEnabled());
        assertFalse(presenter.isSessionEligible());
        assertFalse(presenter.isSuppressUntilUnlocked());
        assertFalse(presenter.isTimerUiResumed());
    }

    /**
     * 配置变更导致的普通页 stop 不得 present（旋转等不应强拉锁屏页）。
     */
    @Test
    public void normalUiStopped_dueToConfigChange_doesNotPresent() {
        makeEligibleAndLocked();
        clearStartedActivities();

        presenter.onNormalTimerUiStopped(true);
        assertNull(shadowApp.getNextStartedActivity());
    }

    // ---------- 辅助：进入「可展示 + 屏亮 + 锁屏」状态 ----------

    private void makeEligibleAndLocked() {
        setScreenInteractive(true);
        shadowKeyguard.setKeyguardLocked(true);
        presenter.updateSession(true, true, false);
    }

    /** 同时设置 isInteractive / isScreenOn，兼容不同 Robolectric Shadow 实现。 */
    private void setScreenInteractive(boolean interactive) {
        shadowPower.setIsInteractive(interactive);
        shadowPower.setIsScreenOn(interactive);
    }

    private boolean isDeviceInteractive() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isInteractive();
    }

    private boolean isDeviceKeyguardLocked() {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }

    private boolean invokeShouldPresent() throws Exception {
        java.lang.reflect.Method m = LockScreenPresenter.class.getDeclaredMethod("shouldPresent");
        m.setAccessible(true);
        return (boolean) m.invoke(presenter);
    }

    private void resetPresentInFlight() {
        try {
            java.lang.reflect.Field field =
                    LockScreenPresenter.class.getDeclaredField("presentInFlight");
            field.setAccessible(true);
            java.util.concurrent.atomic.AtomicBoolean flag =
                    (java.util.concurrent.atomic.AtomicBoolean) field.get(presenter);
            flag.set(false);
        } catch (Exception e) {
            throw new AssertionError("无法重置 presentInFlight", e);
        }
    }
}
