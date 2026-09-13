package com.skyinit.pomodorotimer.domain.timer;

/**
 * 番茄会话阶段（磁盘快照与 UI 共用）。
 */
public enum SessionPhase {
    IDLE,
    RUNNING_STUDY,
    RUNNING_BREAK,
    PAUSED,
    AWAITING_POST_BREAK;

    public boolean isRunning() {
        return this == RUNNING_STUDY || this == RUNNING_BREAK;
    }

    public boolean isActiveSession() {
        return this == RUNNING_STUDY || this == RUNNING_BREAK || this == PAUSED || this == AWAITING_POST_BREAK;
    }

    public static SessionPhase fromFlags(boolean running, boolean paused, int sessionType,
                                         boolean awaitingPostBreakChoice) {
        if (awaitingPostBreakChoice) {
            return AWAITING_POST_BREAK;
        }
        if (paused) {
            return PAUSED;
        }
        if (running) {
            return sessionType == 1 ? RUNNING_BREAK : RUNNING_STUDY;
        }
        return IDLE;
    }
}
