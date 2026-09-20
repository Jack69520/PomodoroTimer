package com.skyinit.pomodorotimer.util;

import android.app.NotificationManager;
import android.content.Context;
import android.os.SystemClock;

import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.data.repository.FocusDndStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNotificationManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link FocusDndHelper}：启用落盘、会话结束无条件恢复（2-A）、孤儿条件恢复（3-A1）。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class FocusDndHelperTest {

    private Context context;
    private NotificationManager nm;
    private ShadowNotificationManager shadowNm;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        FocusDndStore.clear(context);
        ActiveSessionStore.clear(context);
        nm = context.getSystemService(NotificationManager.class);
        shadowNm = Shadows.shadowOf(nm);
        shadowNm.setNotificationPolicyAccessGranted(true);
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
    }

    @Test
    public void maybeEnableDnd_persistsOwnershipAndSetsNone() {
        FocusDndHelper.maybeEnableDnd(context, true);

        assertTrue(FocusDndStore.isOwned(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,
                FocusDndStore.getSavedFilter(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE,
                FocusDndStore.getAppliedFilter(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE,
                nm.getCurrentInterruptionFilter());
    }

    @Test
    public void maybeEnableDnd_skipsWhenUserDisabled() {
        FocusDndHelper.maybeEnableDnd(context, false);
        assertFalse(FocusDndStore.isOwned(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,
                nm.getCurrentInterruptionFilter());
    }

    @Test
    public void maybeEnableDnd_doesNotResampleWhenAlreadyOwned() {
        FocusDndHelper.maybeEnableDnd(context, true);
        // 模拟进程死后系统仍为 NONE、Store 仍 owned；再次 enable 不得把 saved 改成 NONE
        FocusDndHelper.maybeEnableDnd(context, true);
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,
                FocusDndStore.getSavedFilter(context));
    }

    @Test
    public void restoreDnd_restoresSavedAndClearsStore() {
        FocusDndHelper.maybeEnableDnd(context, true);
        FocusDndHelper.restoreDnd(context);

        assertFalse(FocusDndStore.isOwned(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,
                nm.getCurrentInterruptionFilter());
    }

    /**
     * 杀进程语义：仅磁盘有 owned（无任何进程内缓存），restore 仍须写回。
     */
    @Test
    public void restoreDnd_worksFromDiskOnly_simulatingProcessDeath() {
        FocusDndStore.saveOwnership(
                context,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                NotificationManager.INTERRUPTION_FILTER_NONE);
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);

        FocusDndHelper.restoreDnd(context);

        assertFalse(FocusDndStore.isOwned(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                nm.getCurrentInterruptionFilter());
    }

    @Test
    public void restoreDnd_isIdempotent() {
        FocusDndHelper.maybeEnableDnd(context, true);
        FocusDndHelper.restoreDnd(context);
        FocusDndHelper.restoreDnd(context);
        assertFalse(FocusDndStore.isOwned(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,
                nm.getCurrentInterruptionFilter());
    }

    @Test
    public void recoverOrphan_skipsWhenActiveSessionExists() {
        FocusDndStore.saveOwnership(
                context,
                NotificationManager.INTERRUPTION_FILTER_ALL,
                NotificationManager.INTERRUPTION_FILTER_NONE);
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        saveRunningStudySession();

        FocusDndHelper.recoverOrphanIfNeeded(context);

        assertTrue(FocusDndStore.isOwned(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE,
                nm.getCurrentInterruptionFilter());
    }

    @Test
    public void recoverOrphan_restoresWhenStillApplied() {
        FocusDndStore.saveOwnership(
                context,
                NotificationManager.INTERRUPTION_FILTER_ALL,
                NotificationManager.INTERRUPTION_FILTER_NONE);
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);

        FocusDndHelper.recoverOrphanIfNeeded(context);

        assertFalse(FocusDndStore.isOwned(context));
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,
                nm.getCurrentInterruptionFilter());
    }

    @Test
    public void recoverOrphan_clearsOnlyWhenUserChangedFilter() {
        FocusDndStore.saveOwnership(
                context,
                NotificationManager.INTERRUPTION_FILTER_ALL,
                NotificationManager.INTERRUPTION_FILTER_NONE);
        // 用户杀进程后在系统设置中关掉勿扰
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);

        FocusDndHelper.recoverOrphanIfNeeded(context);

        assertFalse(FocusDndStore.isOwned(context));
        // 尊重用户：不得改回其它值（此处仍为 ALL）
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL,
                nm.getCurrentInterruptionFilter());
    }

    private void saveRunningStudySession() {
        long now = SystemClock.elapsedRealtime();
        ActiveSessionStore.save(
                context,
                "user_test",
                1L,
                1,
                now + 60_000L,
                60_000L,
                true,
                false,
                0,
                System.currentTimeMillis(),
                0,
                0L,
                25 * 60_000L,
                -1,
                -1,
                "测试任务",
                "学习",
                "",
                "",
                false,
                false
        );
    }
}
