package com.skyinit.pomodorotimer.ui.home.recovery;

/** 会话恢复用户意图。 */
public enum SessionRecoveryIntent {
    EVALUATE_STARTUP,
    RETRY_EVALUATE,
    SERVICE_READY,
    OPEN_TIMER,
    START_NEW_ROUND,
    DISMISS,
    ACK_DISCARD
}
