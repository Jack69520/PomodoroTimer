package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.util.AppExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class StatisticsRepositoryAccountingTest {

    private Context context;
    private StatisticsRepository repository;
    private String userId;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        ((App) context).initializeAfterConsentSyncForTest();
        userId = "stat_acc_user";
        insertUser(userId);
        repository = new StatisticsRepository(context, AccountManager.getInstance(context));
    }

    private void insertUser(String id) {
        User user = new User();
        user.userId = id;
        user.nickname = id;
        AppDatabase.getDatabase(context).userDao().insert(user);
    }

    @Test
    public void recordSession_writesActiveDurationAndWallEnd() throws Exception {
        long start = 1_700_000_000_000L;
        long active = 25L * 60L * 1000L;
        long paused = 8L * 60L * 1000L;
        long end = start + active + paused;
        repository.recordSession(
                userId, start, active, -1, -1, "学习", null,
                1, Collections.singletonList("喝水"), false, null,
                end, paused, active);

        drainDiskIo();
        PomodoroSession session = latestSession();
        assertTrue(session.completed);
        assertEquals(active, session.duration);
        assertEquals(end, session.endTime);
        assertEquals(paused, session.pausedDurationMs);
        assertEquals(active, session.plannedDurationMs);
        assertTrue(session.endTime > start + session.duration);
    }

    @Test
    public void recordFailedSession_writesIncompleteWithAccounting() throws Exception {
        long start = 1_700_000_100_000L;
        long active = 15L * 60L * 1000L;
        long paused = 5L * 60L * 1000L;
        long planned = 25L * 60L * 1000L;
        long end = start + active + paused;
        repository.recordFailedSession(
                userId, start, active, "暂停超时", -1, "学习",
                1, Collections.singletonList("暂停超时"), null,
                end, paused, planned);

        drainDiskIo();
        PomodoroSession session = latestSession();
        assertFalse(session.completed);
        assertTrue(session.earlyEnd);
        assertEquals(active, session.duration);
        assertEquals(paused, session.pausedDurationMs);
        assertEquals(planned, session.plannedDurationMs);
        assertEquals(end, session.endTime);
    }

    private void drainDiskIo() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AppExecutors.getInstance().diskIo(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        ShadowLooper.idleMainLooper();
    }

    private PomodoroSession latestSession() {
        android.database.Cursor c = AppDatabase.getDatabase(context).getOpenHelper()
                .getReadableDatabase()
                .query("SELECT id FROM pomodoro_sessions WHERE userId=? ORDER BY id DESC LIMIT 1",
                        new String[]{userId});
        assertTrue("no session row for " + userId, c.moveToFirst());
        int id = c.getInt(0);
        c.close();
        PomodoroSession session = AppDatabase.getDatabase(context)
                .pomodoroSessionDao()
                .getSessionByIdSync(id);
        assertTrue(session != null);
        return session;
    }
}
