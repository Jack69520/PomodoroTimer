package com.skyinit.pomodorotimer.domain.ringtone;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 进程级门闩：自定义铃声降级冷启动提示每进程最多一次。
 */
public final class RingtoneAccessPromptGate {

    private static final AtomicBoolean COLD_START_PROMPTED = new AtomicBoolean(false);

    private RingtoneAccessPromptGate() {
    }

    /** @return true 表示本次可展示（首次占用成功） */
    public static boolean tryClaimColdStartPrompt() {
        return COLD_START_PROMPTED.compareAndSet(false, true);
    }

    public static boolean wasColdStartPrompted() {
        return COLD_START_PROMPTED.get();
    }

    /** 仅供单元测试重置。 */
    public static void resetForTests() {
        COLD_START_PROMPTED.set(false);
    }
}
