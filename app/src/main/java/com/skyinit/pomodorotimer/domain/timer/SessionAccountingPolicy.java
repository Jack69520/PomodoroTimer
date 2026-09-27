package com.skyinit.pomodorotimer.domain.timer;

/**
 * 会话记账纯函数：有效专注 = planned − remaining；暂停累计用 elapsed 段累加。
 * <p>
 * 不含 Android 依赖，供 {@code TimerService} 结算前快照调用。
 */
public final class SessionAccountingPolicy {

    private SessionAccountingPolicy() {
    }

    /**
     * @param plannedDurationMs       开局计划时长；&lt;=0 时 active 记 0（无法可靠推断）
     * @param remainingMs             结算前剩余学习时长（暂停中为冻结值）
     * @param accumulatedPausedMs     已结束暂停段累计（不含当前未结束段）
     * @param currentPauseElapsedMs   当前暂停段已流逝；仅 {@code isPaused=true} 时计入
     * @param isPaused                结算瞬间是否仍处于暂停
     * @param nowWallClockMs          当前墙钟
     * @param sessionStartWallMs      会话开始墙钟
     */
    public static SessionAccountingResult settle(long plannedDurationMs,
                                                 long remainingMs,
                                                 long accumulatedPausedMs,
                                                 long currentPauseElapsedMs,
                                                 boolean isPaused,
                                                 long nowWallClockMs,
                                                 long sessionStartWallMs) {
        long planned = Math.max(0L, plannedDurationMs);
        long remaining = Math.max(0L, remainingMs);
        long active;
        if (planned <= 0L) {
            active = 0L;
        } else if (remaining >= planned) {
            active = 0L;
        } else {
            active = planned - remaining;
        }

        long pausedAcc = Math.max(0L, accumulatedPausedMs);
        long current = isPaused ? Math.max(0L, currentPauseElapsedMs) : 0L;
        long paused = pausedAcc + current;

        long endTime = Math.max(nowWallClockMs, Math.max(0L, sessionStartWallMs));

        return new SessionAccountingResult(active, paused, planned, endTime);
    }

    /** 有效专注：{@code max(0, planned - remaining)}，planned&lt;=0 时返回 0。 */
    public static long resolveActiveDurationMs(long plannedDurationMs, long remainingMs) {
        return settle(plannedDurationMs, remainingMs, 0L, 0L, false, 0L, 0L).activeDurationMs;
    }

    public static long resolvePausedDurationMs(long accumulatedPausedMs,
                                               long currentPauseElapsedMs,
                                               boolean isPaused) {
        return settle(0L, 0L, accumulatedPausedMs, currentPauseElapsedMs, isPaused, 0L, 0L)
                .pausedDurationMs;
    }
}
