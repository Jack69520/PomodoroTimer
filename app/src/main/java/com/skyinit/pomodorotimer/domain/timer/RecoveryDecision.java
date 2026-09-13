package com.skyinit.pomodorotimer.domain.timer;

/**
 * 启动时会话恢复决策（纯数据，无 Android 依赖）。
 */
public final class RecoveryDecision {

    public enum Kind {
        NONE,
        CONTINUE_ACTIVE,
        SETTLE_THEN_RESULT,
        SHOW_RESULT,
        DISCARD_FOREIGN_OR_GUEST
    }

    public enum ResultKind {
        NONE,
        COMPLETED,
        FAILED_TIMEOUT,
        BREAK_STARTED
    }

    public final Kind kind;
    public final ResultKind resultKind;
    public final long sessionId;

    private RecoveryDecision(Kind kind, ResultKind resultKind, long sessionId) {
        this.kind = kind != null ? kind : Kind.NONE;
        this.resultKind = resultKind != null ? resultKind : ResultKind.NONE;
        this.sessionId = sessionId;
    }

    public static RecoveryDecision none() {
        return new RecoveryDecision(Kind.NONE, ResultKind.NONE, 0L);
    }

    public static RecoveryDecision continueActive(long sessionId) {
        return new RecoveryDecision(Kind.CONTINUE_ACTIVE, ResultKind.NONE, sessionId);
    }

    public static RecoveryDecision settleThenResult(long sessionId) {
        return new RecoveryDecision(Kind.SETTLE_THEN_RESULT, ResultKind.NONE, sessionId);
    }

    public static RecoveryDecision showResult(ResultKind resultKind, long sessionId) {
        return new RecoveryDecision(Kind.SHOW_RESULT, resultKind, sessionId);
    }

    public static RecoveryDecision discardForeignOrGuest() {
        return new RecoveryDecision(Kind.DISCARD_FOREIGN_OR_GUEST, ResultKind.NONE, 0L);
    }
}
