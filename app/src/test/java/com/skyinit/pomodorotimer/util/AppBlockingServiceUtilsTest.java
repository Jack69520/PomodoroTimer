package com.skyinit.pomodorotimer.util;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.service.AppBlockingService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link AppBlockingServiceUtils} 单元测试。
 * <p>
 * 工具类负责把「独立屏蔽 / 计时联动」翻译成带 action、source、sessionStartTime
 * 的 Service Intent。此处验证 Intent 契约，避免调用方传错 Extra 导致服务误启停。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class AppBlockingServiceUtilsTest {

    private Context context;
    private ShadowApplication shadowApp;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        shadowApp = Shadows.shadowOf((Application) context);
        drainStartedServices();
    }

    /** 清空 Shadow 已记录的 startService，保证断言只看到本用例发出的 Intent。 */
    private void drainStartedServices() {
        while (shadowApp.getNextStartedService() != null) {
            // drain
        }
    }

    /** 独立屏蔽启动：action=start，source=standalone，无会话时间。 */
    @Test
    public void startStandaloneBlocking_sendsCorrectExtras() {
        AppBlockingServiceUtils.startStandaloneBlocking(context);

        Intent intent = shadowApp.getNextStartedService();
        assertNotNull(intent);
        assertEquals(AppBlockingService.class.getName(), intent.getComponent().getClassName());
        assertEquals(AppBlockingServiceUtils.ACTION_START_BLOCKING,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_ACTION));
        assertEquals(AppBlockingServiceUtils.SOURCE_STANDALONE,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE));
        // sessionStartTime 仅在 >0 时写入；独立屏蔽不应带该 Extra
        assertEquals(0L, intent.getLongExtra(AppBlockingServiceUtils.EXTRA_SESSION_START_TIME, 0L));
    }

    /** 独立屏蔽停止：action=stop，source=standalone。 */
    @Test
    public void stopStandaloneBlocking_sendsStopWithStandaloneSource() {
        AppBlockingServiceUtils.stopStandaloneBlocking(context);

        Intent intent = shadowApp.getNextStartedService();
        assertNotNull(intent);
        assertEquals(AppBlockingServiceUtils.ACTION_STOP_BLOCKING,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_ACTION));
        assertEquals(AppBlockingServiceUtils.SOURCE_STANDALONE,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE));
    }

    /** 计时联动启动：应带上 sessionStartTime，供拦截记录归属会话。 */
    @Test
    public void startTimerBlocking_includesSessionStartTime() {
        long sessionStart = 1_700_000_000_000L;
        AppBlockingServiceUtils.startTimerBlocking(context, sessionStart);

        Intent intent = shadowApp.getNextStartedService();
        assertNotNull(intent);
        assertEquals(AppBlockingServiceUtils.ACTION_START_BLOCKING,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_ACTION));
        assertEquals(AppBlockingServiceUtils.SOURCE_TIMER,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE));
        assertEquals(sessionStart,
                intent.getLongExtra(AppBlockingServiceUtils.EXTRA_SESSION_START_TIME, -1L));
    }

    /** sessionStartTime<=0 时不应写入 Extra（避免脏 0 污染记录解析）。 */
    @Test
    public void startTimerBlocking_zeroSessionStart_omitsExtra() {
        AppBlockingServiceUtils.startTimerBlocking(context, 0L);

        Intent intent = shadowApp.getNextStartedService();
        assertNotNull(intent);
        assertEquals(AppBlockingServiceUtils.SOURCE_TIMER,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE));
        // putExtra 未调用时 getLongExtra 返回默认值
        assertEquals(-1L, intent.getLongExtra(AppBlockingServiceUtils.EXTRA_SESSION_START_TIME, -1L));
    }

    /** 计时联动停止：source 必须为 timer，避免误停独立屏蔽来源。 */
    @Test
    public void stopTimerBlocking_sendsStopWithTimerSource() {
        AppBlockingServiceUtils.stopTimerBlocking(context);

        Intent intent = shadowApp.getNextStartedService();
        assertNotNull(intent);
        assertEquals(AppBlockingServiceUtils.ACTION_STOP_BLOCKING,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_ACTION));
        assertEquals(AppBlockingServiceUtils.SOURCE_TIMER,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE));
    }

    /** 缓存刷新：仅发 refresh_cache，不改变来源开关。 */
    @Test
    public void notifyCacheRefresh_sendsRefreshAction() {
        AppBlockingServiceUtils.notifyCacheRefresh(context);

        Intent intent = shadowApp.getNextStartedService();
        assertNotNull(intent);
        assertEquals(AppBlockingServiceUtils.ACTION_REFRESH_CACHE,
                intent.getStringExtra(AppBlockingServiceUtils.EXTRA_ACTION));
        assertNull(intent.getStringExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE));
    }

    /** 常量契约：防止重构时改名导致跨模块 Intent 断裂。 */
    @Test
    public void publicConstants_keepStableContract() {
        assertEquals("action", AppBlockingServiceUtils.EXTRA_ACTION);
        assertEquals("blocking_source", AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE);
        assertEquals("session_start_time", AppBlockingServiceUtils.EXTRA_SESSION_START_TIME);
        assertEquals("start_blocking", AppBlockingServiceUtils.ACTION_START_BLOCKING);
        assertEquals("stop_blocking", AppBlockingServiceUtils.ACTION_STOP_BLOCKING);
        assertEquals("refresh_cache", AppBlockingServiceUtils.ACTION_REFRESH_CACHE);
        assertEquals("standalone", AppBlockingServiceUtils.SOURCE_STANDALONE);
        assertEquals("timer", AppBlockingServiceUtils.SOURCE_TIMER);
        assertTrue(true);
    }
}
