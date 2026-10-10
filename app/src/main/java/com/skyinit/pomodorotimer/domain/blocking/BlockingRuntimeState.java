package com.skyinit.pomodorotimer.domain.blocking;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 应用屏蔽运行态（L3）：进程内原子真相源。
 * <p>
 * 仅 {@link com.skyinit.pomodorotimer.service.AppBlockingService} 可写；
 * UI / Utils / Reconciler 只读。进程死亡后全部清零，与 Room 意图（L1）脱节时即为 orphan。
 */
public final class BlockingRuntimeState {

    private static final AtomicBoolean MONITORING = new AtomicBoolean(false);
    private static final AtomicBoolean STANDALONE = new AtomicBoolean(false);
    private static final AtomicBoolean TIMER = new AtomicBoolean(false);
    private static final AtomicLong TIMER_SESSION_START = new AtomicLong(0L);
    private static final AtomicReference<String> BLOCKING_USER_ID = new AtomicReference<>(null);

    private BlockingRuntimeState() {
    }

    public static boolean isMonitoringActive() {
        return MONITORING.get();
    }

    public static boolean isStandaloneActive() {
        return STANDALONE.get();
    }

    public static boolean isTimerActive() {
        return TIMER.get();
    }

    public static long getTimerSessionStartTime() {
        return TIMER_SESSION_START.get();
    }

    @Nullable
    public static String getBlockingUserId() {
        return BLOCKING_USER_ID.get();
    }

    public static boolean isAnySourceActive() {
        return STANDALONE.get() || TIMER.get();
    }

    /** Service 写：同步来源与监控标志。 */
    public static void publish(boolean monitoring,
                               boolean standalone,
                               boolean timer,
                               long timerSessionStartTime,
                               @Nullable String blockingUserId) {
        MONITORING.set(monitoring);
        STANDALONE.set(standalone);
        TIMER.set(timer);
        TIMER_SESSION_START.set(timer ? Math.max(0L, timerSessionStartTime) : 0L);
        BLOCKING_USER_ID.set(blockingUserId);
    }

    /** Service 写：进程销毁或服务完全停止时清空。 */
    public static void clear() {
        publish(false, false, false, 0L, null);
    }

    /** 测试辅助：复位静态状态。 */
    public static void resetForTests() {
        clear();
    }
}
