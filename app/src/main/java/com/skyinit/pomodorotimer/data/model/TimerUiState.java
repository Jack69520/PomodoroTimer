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

    public TimerUiState(long timeLeftMillis, boolean running, boolean paused, int sessionType,
                        boolean awaitingPostBreakChoice, boolean longBreak) {
        this.timeLeftMillis = timeLeftMillis;
        this.running = running;
        this.paused = paused;
        this.sessionType = sessionType;
        this.awaitingPostBreakChoice = awaitingPostBreakChoice;
        this.longBreak = longBreak;
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
