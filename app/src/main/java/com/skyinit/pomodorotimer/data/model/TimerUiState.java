package com.skyinit.pomodorotimer.data.model;

/**
 * 计时器 UI 状态快照，由 {@link com.skyinit.pomodorotimer.data.repository.TimerStateRepository} 发布。
 */
public final class TimerUiState {

    /** sessionType: 0 = 学习, 1 = 休息（含长休息） */
    public static final int SESSION_STUDY = 0;
    public static final int SESSION_BREAK = 1;

    public final long timeLeftMillis;
    public final boolean running;
    public final boolean paused;
    public final int sessionType;
    /** 休息结束后等待用户选择是否开始新一轮番茄。 */
    public final boolean awaitingPostBreakChoice;
    /** 当前休息是否为长休息。 */
    public final boolean longBreak;
    /** 会话 ID（通常为 sessionStartTime）。 */
    public final long sessionId;
    public final int generation;
    /** 暂停态下距离超时失败的剩余毫秒；非暂停为 0。 */
    public final long pauseTimeoutRemainingMs;
    public final boolean exactAlarmReliable;
    /** 当前学习会话是否仍可暂停（已达上限时为 false）。 */
    public final boolean canPause;

    public TimerUiState(long timeLeftMillis, boolean running, boolean paused, int sessionType,
                        boolean awaitingPostBreakChoice, boolean longBreak) {
        this(timeLeftMillis, running, paused, sessionType, awaitingPostBreakChoice, longBreak,
                0L, 0, 0L, true, false);
    }

    public TimerUiState(long timeLeftMillis, boolean running, boolean paused, int sessionType,
                        boolean awaitingPostBreakChoice, boolean longBreak,
                        long sessionId, int generation,
                        long pauseTimeoutRemainingMs, boolean exactAlarmReliable) {
        this(timeLeftMillis, running, paused, sessionType, awaitingPostBreakChoice, longBreak,
                sessionId, generation, pauseTimeoutRemainingMs, exactAlarmReliable, false);
    }

    public TimerUiState(long timeLeftMillis, boolean running, boolean paused, int sessionType,
                        boolean awaitingPostBreakChoice, boolean longBreak,
                        long sessionId, int generation,
                        long pauseTimeoutRemainingMs, boolean exactAlarmReliable,
                        boolean canPause) {
        this.timeLeftMillis = timeLeftMillis;
        this.running = running;
        this.paused = paused;
        this.sessionType = sessionType;
        this.awaitingPostBreakChoice = awaitingPostBreakChoice;
        this.longBreak = longBreak;
        this.sessionId = sessionId;
        this.generation = generation;
        this.pauseTimeoutRemainingMs = pauseTimeoutRemainingMs;
        this.exactAlarmReliable = exactAlarmReliable;
        this.canPause = canPause;
    }

    public boolean isBreakSession() {
        return sessionType == SESSION_BREAK;
    }

    public boolean isStudySession() {
        return sessionType == SESSION_STUDY;
    }

    public static TimerUiState idle(long defaultStudyTimeMs) {
        return new TimerUiState(defaultStudyTimeMs, false, false, SESSION_STUDY, false, false);
    }
}
