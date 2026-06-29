package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import androidx.room.Room;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class AccountDataIsolationTest {

    private AppDatabase database;

    @Before
    public void setUp() {
        Context context = org.robolectric.RuntimeEnvironment.getApplication();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void pomodoroSessions_isolatedByUserId() {
        insertCompletedSession("user_a", 1000L);
        insertCompletedSession("user_a", 2000L);
        insertCompletedSession("user_b", 3000L);

        assertEquals(2, database.pomodoroSessionDao().getTotalCompletedCountForUser("user_a"));
        assertEquals(1, database.pomodoroSessionDao().getTotalCompletedCountForUser("user_b"));
    }

    @Test
    public void userPomodoroSettings_isolatedPerUser() {
        UserPomodoroSettings settingsA = new UserPomodoroSettings("user_a");
        settingsA.defaultStudyTimeMs = 30L * 60L * 1000L;
        settingsA.maxPauseCount = 3;

        UserPomodoroSettings settingsB = new UserPomodoroSettings("user_b");
        settingsB.defaultStudyTimeMs = 45L * 60L * 1000L;
        settingsB.maxPauseCount = 1;

        database.userPomodoroSettingsDao().upsert(settingsA);
        database.userPomodoroSettingsDao().upsert(settingsB);

        UserPomodoroSettings loadedA = database.userPomodoroSettingsDao().getByUserId("user_a");
        UserPomodoroSettings loadedB = database.userPomodoroSettingsDao().getByUserId("user_b");

        assertEquals(30L * 60L * 1000L, loadedA.defaultStudyTimeMs);
        assertEquals(45L * 60L * 1000L, loadedB.defaultStudyTimeMs);
        assertNotEquals(loadedA.maxPauseCount, loadedB.maxPauseCount);
    }

    @Test
    public void registeredUsers_doNotShareSessionCounts() {
        User user1 = new User();
        user1.userId = "reg_1";
        user1.accountType = User.ACCOUNT_TYPE_REGISTERED;
        user1.nickname = "A";

        User user2 = new User();
        user2.userId = "reg_2";
        user2.accountType = User.ACCOUNT_TYPE_REGISTERED;
        user2.nickname = "B";

        database.userDao().insert(user1);
        database.userDao().insert(user2);

        insertCompletedSession("reg_1", 5000L);
        insertCompletedSession("reg_2", 6000L);
        insertCompletedSession("reg_2", 7000L);

        assertEquals(1, database.pomodoroSessionDao().getTotalCompletedCountForUser("reg_1"));
        assertEquals(2, database.pomodoroSessionDao().getTotalCompletedCountForUser("reg_2"));
    }

    private void insertCompletedSession(String userId, long durationMs) {
        PomodoroSession session = new PomodoroSession();
        session.userId = userId;
        session.startTime = System.currentTimeMillis();
        session.endTime = session.startTime + durationMs;
        session.duration = durationMs;
        session.completed = true;
        session.taskId = -1;
        session.subTaskId = -1;
        database.pomodoroSessionDao().insert(session);
    }
}
