package com.skyinit.pomodorotimer.service;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.AppDatabase;
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

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class TimerServiceTest {

    private Context context;
    private TimerService service;

    @Before
    public void setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        ((App) context).initializeAfterConsentSyncForTest();
        ActiveSessionStore.clear(context);

        ServiceController<TimerService> controller = Robolectric.buildService(TimerService.class);
        service = controller.create().get();
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
        Intent start = serviceIntent(TimerService.ACTION_START);
        start.putExtra(TimerService.EXTRA_STUDY_DURATION_MS, 60_000L);
        service.onStartCommand(start, 0, 1);
        assertTrue(service.isRunning());
        assertEquals(0, service.getSessionType());

        ActiveSessionStore.clear(context);
        forceTimerExpired(service);

        Intent alarm = serviceIntent(TimerService.ACTION_SESSION_COMPLETE);
        service.onStartCommand(alarm, 0, 2);

        // Alarm 兜底完成学习后自动进入休息阶段
        assertTrue(service.isRunning());
        assertEquals(1, service.getSessionType());
    }

    /** 将内存中的计时终点设为已过期，用于 Alarm 兜底路径测试。 */
    private void forceTimerExpired(TimerService service) throws Exception {
        Field endField = TimerService.class.getDeclaredField("timerEndElapsedRealtime");
        endField.setAccessible(true);
        endField.setLong(service, SystemClock.elapsedRealtime() - 1L);
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

    private void deliver(String action) {
        service.onStartCommand(serviceIntent(action), 0, 1);
    }

    private Intent serviceIntent(String action) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(action);
        return intent;
    }
}
