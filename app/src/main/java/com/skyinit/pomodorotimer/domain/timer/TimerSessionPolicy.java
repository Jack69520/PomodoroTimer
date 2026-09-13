package com.skyinit.pomodorotimer.domain.timer;

/**
 * 纯函数会话策略：根据阶段与时钟决定是否结算，不触碰 Android 框架。
 */
public final class TimerSessionPolicy {

    public static final long DEFAULT_PAUSE_TIMEOUT_MS = 5L * 60L * 1000L;

    private TimerSessionPolicy() {
    }

    /**
     * @param expectedGeneration Alarm Intent 携带的代际；&lt;0 表示不做代际校验
     * @param snapshotGeneration 快照当前代际
     * @param pauseTimeoutMs     暂停超时阈值
     */
    public static SettleDecision decide(SessionPhase phase,
                                        long remainingMs,
                                        long pauseElapsedMs,
                                        long pauseTimeoutMs,
                                        int expectedGeneration,
                                        int snapshotGeneration) {
        if (phase == null || phase == SessionPhase.IDLE) {
            return SettleDecision.IGNORE;
        }
        if (expectedGeneration >= 0 && expectedGeneration != snapshotGeneration) {
            return SettleDecision.IGNORE;
        }
        if (phase == SessionPhase.PAUSED) {
            if (pauseElapsedMs >= pauseTimeoutMs) {
                return SettleDecision.FAIL_PAUSE_TIMEOUT;
            }
            return SettleDecision.NONE;
        }
        if (phase.isRunning()) {
            if (remainingMs <= 0L) {
                return SettleDecision.COMPLETE_RUNNING;
            }
            return SettleDecision.NONE;
        }
        return SettleDecision.NONE;
    }

    public static long resolveRunningRemaining(long endAtElapsedRealtime,
                                               long endAtWallClockMs,
                                               long nowElapsedRealtime,
                                               long nowWallClockMs) {
        boolean preferWall = SessionClock.shouldPreferWallClock(
                endAtElapsedRealtime, endAtWallClockMs, nowElapsedRealtime, nowWallClockMs);
        return SessionClock.computeRunningRemainingMs(
                endAtElapsedRealtime, endAtWallClockMs, nowElapsedRealtime, nowWallClockMs, preferWall);
    }

    public static long resolvePauseElapsed(long pauseStartElapsedRealtime,
                                           long pauseStartWallClockMs,
                                           long nowElapsedRealtime,
                                           long nowWallClockMs) {
        boolean preferWall = SessionClock.shouldPreferWallForPause(
                pauseStartElapsedRealtime, pauseStartWallClockMs, nowElapsedRealtime, nowWallClockMs);
        return SessionClock.computePauseElapsedMs(
                pauseStartElapsedRealtime,
                pauseStartWallClockMs,
                nowElapsedRealtime,
                nowWallClockMs,
                preferWall);
    }
}
