package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.content.Intent;

import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.ui.home.TimerActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link FocusBlockNavigation} 单元测试。
 * <p>
 * 屏蔽触发后的「回哪里」由是否存在活跃番茄会话决定：
 * 计时中 → {@link TimerActivity}；仅独立屏蔽 →「我的」页（MainActivity + nav_profile）。
 * 该分支直接影响用户被拦后的体验，需保持与产品文档一致。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class FocusBlockNavigationTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        ActiveSessionStore.clear(context);
    }

    /** 无活跃会话：不应回计时页。 */
    @Test
    public void shouldReturnToTimer_false_whenNoActiveSession() {
        assertFalse(FocusBlockNavigation.shouldReturnToTimer(context));
    }

    /** 写入活跃学习会话后：应回计时页。 */
    @Test
    public void shouldReturnToTimer_true_whenActiveSessionExists() {
        saveRunningStudySession();
        assertTrue(FocusBlockNavigation.shouldReturnToTimer(context));
    }

    /** 无会话时 createReturnIntent 指向「我的」页。 */
    @Test
    public void createReturnIntent_goesToProfile_whenNoSession() {
        Intent intent = FocusBlockNavigation.createReturnIntent(context);
        assertEquals(MainActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(R.id.nav_profile,
                intent.getIntExtra(FocusBlockNavigation.EXTRA_NAV_DESTINATION, -1));
    }

    /** 有活跃会话时 createReturnIntent 指向计时页。 */
    @Test
    public void createReturnIntent_goesToTimer_whenSessionActive() {
        saveRunningStudySession();
        Intent intent = FocusBlockNavigation.createReturnIntent(context);
        assertEquals(TimerActivity.class.getName(), intent.getComponent().getClassName());
    }

    /**
     * 写入一条「运行中学习」快照，供导航分支断言使用。
     * 参数只需满足 {@link ActiveSessionStore#hasActiveSession}（KEY_ACTIVE=true）。
     */
    private void saveRunningStudySession() {
        long now = android.os.SystemClock.elapsedRealtime();
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
