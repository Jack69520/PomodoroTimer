package com.skyinit.pomodorotimer.domain.timer;

/**
 * 学习会话结算记账结果（有效专注 / 暂停累计 / 计划时长 / 墙钟结束）。
 */
public final class SessionAccountingResult {

    public final long activeDurationMs;
    public final long pausedDurationMs;
    public final long plannedDurationMs;
    public final long endTimeMs;

    public SessionAccountingResult(long activeDurationMs,
                                   long pausedDurationMs,
                                   long plannedDurationMs,
                                   long endTimeMs) {
        this.activeDurationMs = Math.max(0L, activeDurationMs);
        this.pausedDurationMs = Math.max(0L, pausedDurationMs);
        this.plannedDurationMs = Math.max(0L, plannedDurationMs);
        this.endTimeMs = endTimeMs;
    }
}
