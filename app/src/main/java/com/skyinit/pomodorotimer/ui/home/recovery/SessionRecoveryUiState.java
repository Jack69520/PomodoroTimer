package com.skyinit.pomodorotimer.ui.home.recovery;

import com.skyinit.pomodorotimer.domain.timer.RecoveryDecision;

/**
 * 启动会话恢复 UI 状态（MVI）。
 */
public final class SessionRecoveryUiState {

    public enum Phase {
        IDLE,
        EVALUATING,
        AWAITING_SERVICE,
        CONTINUE_READY,
        RESULT,
        DISCARDED,
        FAILED
    }

    public final Phase phase;
    public final RecoveryDecision.ResultKind resultKind;
    public final long sessionId;
    public final boolean blocking;

    public SessionRecoveryUiState(Phase phase,
                                  RecoveryDecision.ResultKind resultKind,
                                  long sessionId,
                                  boolean blocking) {
        this.phase = phase != null ? phase : Phase.IDLE;
        this.resultKind = resultKind != null ? resultKind : RecoveryDecision.ResultKind.NONE;
        this.sessionId = sessionId;
        this.blocking = blocking;
    }

    public static SessionRecoveryUiState idle() {
        return new SessionRecoveryUiState(Phase.IDLE, RecoveryDecision.ResultKind.NONE, 0L, false);
    }

    public SessionRecoveryUiState withPhase(Phase phase) {
        return new SessionRecoveryUiState(phase, resultKind, sessionId, blocking);
    }
}
