package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import androidx.room.Room;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.entity.UserAppBlocking;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 多注册用户数据隔离：番茄记录、屏蔽开关、待办删除不影响计时记录。
 */
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
        insertUser("user_a");
        insertUser("user_b");
        insertCompletedSession("user_a", 1000L);
        insertCompletedSession("user_a", 2000L);
        insertCompletedSession("user_b", 3000L);

        assertEquals(2, database.pomodoroSessionDao().getTotalCompletedCountForUser("user_a"));
        assertEquals(1, database.pomodoroSessionDao().getTotalCompletedCountForUser("user_b"));
    }

    @Test
    public void userAppBlocking_isolatedPerUser() {
        insertUser("user_a");
        insertUser("user_b");

        UserAppBlocking blockingA = new UserAppBlocking();
        blockingA.userId = "user_a";
        blockingA.enabled = true;
        UserAppBlocking blockingB = new UserAppBlocking();
        blockingB.userId = "user_b";
        blockingB.enabled = false;

        database.userAppBlockingDao().upsert(blockingA);
        database.userAppBlockingDao().upsert(blockingB);

        UserAppBlocking loadedA = database.userAppBlockingDao().getByUserId("user_a");
        UserAppBlocking loadedB = database.userAppBlockingDao().getByUserId("user_b");

        assertTrue(loadedA.enabled);
        assertFalse(loadedB.enabled);
    }

    @Test
    public void registeredUsers_doNotShareSessionCounts() {
        insertUser("reg_1");
        insertUser("reg_2");

        insertCompletedSession("reg_1", 5000L);
        insertCompletedSession("reg_2", 6000L);
        insertCompletedSession("reg_2", 7000L);

        assertEquals(1, database.pomodoroSessionDao().getTotalCompletedCountForUser("reg_1"));
        assertEquals(2, database.pomodoroSessionDao().getTotalCompletedCountForUser("reg_2"));
    }

    /**
     * 删除待办后计时记录应保留（本项目以计时为核心，taskId 仅为历史引用）。
     */
    @Test
    public void deleteTodo_preservesPomodoroSessions() {
        insertUser("user_timer");

        TodoItem todo = new TodoItem("Focus task");
        todo.userId = "user_timer";
        long todoId = database.todoDao().insert(todo);

        PomodoroSession session = new PomodoroSession();
        session.userId = "user_timer";
        session.startTime = System.currentTimeMillis();
        session.endTime = session.startTime + 1500_000L;
        session.duration = 1500_000L;
        session.completed = true;
        session.taskId = (int) todoId;
        database.pomodoroSessionDao().insert(session);

        database.subTaskDao().deleteSubtasksByParentId((int) todoId);
        database.todoDao().delete(database.todoDao().getTodoByIdSync((int) todoId));

        assertNull(database.todoDao().getTodoByIdSync((int) todoId));
        assertEquals(1, database.pomodoroSessionDao().getTotalCompletedCountForUser("user_timer"));
        assertEquals((int) todoId, session.taskId);
    }

    private void insertUser(String userId) {
        User user = new User();
        user.userId = userId;
        user.nickname = userId;
        database.userDao().insert(user);
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
