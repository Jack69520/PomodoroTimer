package com.skyinit.pomodorotimer.service;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.util.AppExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class TimerServiceTest {

    private static final String TEST_PASSWORD = "Abcdef1";
    private static final long CALLBACK_TIMEOUT_MS = 10_000L;

    private Context context;
    private TimerService service;

    @Before
    public void setUp() throws InterruptedException {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        ((App) context).initializeAfterConsentSyncForTest();
        ActiveSessionStore.clear(context);
        ensureLoggedIn();

        ServiceController<TimerService> controller = Robolectric.buildService(TimerService.class);
        service = controller.create().get();
    }

    /** ACTION_START 要求已登录会话，与正式产品路径一致。 */
    private void ensureLoggedIn() throws InterruptedException {
        AccountManager accountManager = AccountManager.getInstance(context);
        if (accountManager.hasActiveSession()) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        accountManager.register("TimerTester", TEST_PASSWORD, TEST_PASSWORD, null, null,
                new AccountManager.RegisterCallback() {
                    @Override
                    public void onSuccess(User user) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        error.set(message);
                        latch.countDown();
                    }
                });
        long deadline = SystemClock.elapsedRealtime() + CALLBACK_TIMEOUT_MS;
        while (latch.getCount() > 0 && SystemClock.elapsedRealtime() < deadline) {
            ShadowLooper.idleMainLooper();
            latch.await(50, TimeUnit.MILLISECONDS);
        }
        if (latch.getCount() > 0) {
            fail("Timed out registering test user");
        }
        if (error.get() != null) {
            fail("Register failed: " + error.get());
        }
        assertTrue(accountManager.hasActiveSession());
    }

    private void waitForPomodoroSettingsCache() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AppExecutors.getInstance().diskIo(() -> {
            try {
                AppContainer.getInstance(context).getUserPomodoroSettingsRepository().getSettingsSync();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        }, throwable -> {
            failure.compareAndSet(null, throwable);
            latch.countDown();
        });
        assertTrue("Timed out waiting for pomodoro settings cache", latch.await(5, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError("Failed to warm pomodoro settings cache", failure.get());
        }
    }

    @Test
    public void start_pause_resume_studySession() {
        deliver(TimerService.ACTION_START);
        assertTrue(service.isRunning());
        assertFalse(service.isPaused());
        assertEquals(0, service.getSessionType());

        deliver(TimerService.ACTION_PAUSE);
        assertFalse(service.isRunning());
        assertTrue(service.isPaused());

        deliver(TimerService.ACTION_RESUME);
        assertTrue(service.isRunning());
        assertFalse(service.isPaused());
    }

    @Test
    public void sessionCompleteViaAlarm_afterStudyExpires() throws Exception {
        waitForPomodoroSettingsCache();
        Intent start = serviceIntent(TimerService.ACTION_START);
        start.putExtra(TimerService.EXTRA_STUDY_DURATION_MS, 60_000L);
        service.onStartCommand(start, 0, 1);
        assertTrue(service.isRunning());
        assertEquals(0, service.getSessionType());

        ActiveSessionStore.clear(context);
        forceTimerExpired(service);

        Intent alarm = serviceIntent(TimerService.ACTION_SESSION_COMPLETE);
        alarm.putExtra(TimerAlarmScheduler.EXTRA_GENERATION, -1);
        service.onStartCommand(alarm, 0, 2);

        // Alarm 兜底完成学习后自动进入休息阶段
        assertTrue(service.isRunning());
        assertEquals(1, service.getSessionType());
    }

    @Test
    public void onDestroy_keepsCheckpointWhenRunning() throws Exception {
        Intent start = serviceIntent(TimerService.ACTION_START);
        start.putExtra(TimerService.EXTRA_STUDY_DURATION_MS, 60_000L);
        service.onStartCommand(start, 0, 1);
        assertTrue(service.isRunning());
        assertTrue(ActiveSessionStore.hasActiveSession(context));

        service.onDestroy();
        assertTrue("Checkpoint must survive onDestroy", ActiveSessionStore.hasActiveSession(context));
    }

    @Test
    public void evaluateCheckpoint_guestDiscardsForeignSnapshot() {
        ActiveSessionStore.save(
                context,
                "foreignUser",
                12345L,
                1,
                SystemClock.elapsedRealtime() + 60_000L,
                60_000L,
                true,
                false,
                0,
                12345L,
                0,
                0L,
                60_000L,
                -1,
                -1,
                "",
                "学习",
                "",
                "",
                false,
                false
        );
        assertTrue(ActiveSessionStore.hasActiveSession(context));

        deliver(TimerService.ACTION_EVALUATE_CHECKPOINT);

        assertFalse(service.isRunning());
        assertFalse(service.isPaused());
        assertFalse(ActiveSessionStore.hasActiveSession(context));
    }

    @Test
    public void pauseWithReason_respectsMaxPauseCount() throws Exception {
        waitForPomodoroSettingsCache();
        int max = AppContainer.getInstance(context).getTimerSettingsRepository().getMaxPauseCount();

        deliver(TimerService.ACTION_START);
        assertTrue(service.isRunning());

        for (int i = 0; i < max; i++) {
            Intent pause = serviceIntent(TimerService.ACTION_PAUSE_WITH_REASON);
            pause.putExtra("pause_reason", "reason-" + i);
            service.onStartCommand(pause, 0, i + 2);
            assertTrue("pause #" + (i + 1) + " should succeed", service.isPaused());
            assertEquals(i + 1, service.getPauseCount());
            deliver(TimerService.ACTION_RESUME);
            assertTrue(service.isRunning());
        }

        assertFalse(service.canPause());
        assertEquals(0, service.getRemainingPauseCount());

        Intent blocked = serviceIntent(TimerService.ACTION_PAUSE_WITH_REASON);
        blocked.putExtra("pause_reason", "blocked");
        service.onStartCommand(blocked, 0, 100);
        assertTrue("over-limit pause must be rejected", service.isRunning());
        assertFalse(service.isPaused());
        assertEquals(max, service.getPauseCount());
    }

    @Test
    public void notificationPause_respectsMaxPauseCount() throws Exception {
        waitForPomodoroSettingsCache();
        int max = AppContainer.getInstance(context).getTimerSettingsRepository().getMaxPauseCount();

        deliver(TimerService.ACTION_START);
        for (int i = 0; i < max; i++) {
            deliver(TimerService.ACTION_PAUSE);
            assertTrue(service.isPaused());
            deliver(TimerService.ACTION_RESUME);
        }

        assertFalse(service.canPause());
        deliver(TimerService.ACTION_PAUSE);
        assertTrue(service.isRunning());
        assertEquals(max, service.getPauseCount());
    }

    @Test
    public void actionStart_whilePaused_doesNotResetPauseCount() throws Exception {
        waitForPomodoroSettingsCache();

        deliver(TimerService.ACTION_START);
        deliver(TimerService.ACTION_PAUSE);
        assertTrue(service.isPaused());
        assertEquals(1, service.getPauseCount());
        long timeLeftBefore = service.getTimeLeft();

        deliver(TimerService.ACTION_START);

        assertTrue("should remain paused", service.isPaused());
        assertEquals(1, service.getPauseCount());
        assertEquals(timeLeftBefore, service.getTimeLeft());
    }

    @Test
    public void reset_stopsRunningStudySession() {
        deliver(TimerService.ACTION_START);
        assertTrue(service.isRunning());

        deliver(TimerService.ACTION_RESET);
        assertFalse(service.isRunning());
        assertFalse(service.isPaused());
        assertEquals(0, service.getSessionType());
    }

    /** 将内存中的计时终点设为已过期，用于 Alarm 兜底路径测试。 */
    private void forceTimerExpired(TimerService service) throws Exception {
        Field endField = TimerService.class.getDeclaredField("timerEndElapsedRealtime");
        endField.setAccessible(true);
        endField.setLong(service, SystemClock.elapsedRealtime() - 1L);
    }

    private void deliver(String action) {
        service.onStartCommand(serviceIntent(action), 0, 1);
    }

    private Intent serviceIntent(String action) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(action);
        return intent;
    }
}
