package com.skyinit.pomodorotimer.domain.timer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SessionRecoveryPolicyTest {

    private static final long TIMEOUT = TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS;

    @Test
    public void guest_withCheckpoint_discards() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                null, "", 0L,
                true, "userA",
                true, false, false,
                60_000L, 0L, TIMEOUT, 100L);
        assertEquals(RecoveryDecision.Kind.DISCARD_FOREIGN_OR_GUEST, d.kind);
    }

    @Test
    public void guest_withResult_discards() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "", "completed", 9L,
                false, "",
                false, false, false,
                0L, 0L, TIMEOUT, 0L);
        assertEquals(RecoveryDecision.Kind.DISCARD_FOREIGN_OR_GUEST, d.kind);
    }

    @Test
    public void guest_clean_none() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                null, "", 0L,
                false, "",
                false, false, false,
                0L, 0L, TIMEOUT, 0L);
        assertEquals(RecoveryDecision.Kind.NONE, d.kind);
    }

    @Test
    public void wrongUser_discards() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userB", "", 0L,
                true, "userA",
                true, false, false,
                60_000L, 0L, TIMEOUT, 100L);
        assertEquals(RecoveryDecision.Kind.DISCARD_FOREIGN_OR_GUEST, d.kind);
    }

    @Test
    public void runningNotExpired_continues() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userA", "", 0L,
                true, "userA",
                true, false, false,
                60_000L, 0L, TIMEOUT, 100L);
        assertEquals(RecoveryDecision.Kind.CONTINUE_ACTIVE, d.kind);
        assertEquals(100L, d.sessionId);
    }

    @Test
    public void runningExpired_settle() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userA", "", 0L,
                true, "userA",
                true, false, false,
                0L, 0L, TIMEOUT, 100L);
        assertEquals(RecoveryDecision.Kind.SETTLE_THEN_RESULT, d.kind);
    }

    @Test
    public void pausedWithinTimeout_continues() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userA", "", 0L,
                true, "userA",
                false, true, false,
                30_000L, 60_000L, TIMEOUT, 100L);
        assertEquals(RecoveryDecision.Kind.CONTINUE_ACTIVE, d.kind);
    }

    @Test
    public void pausedTimedOut_settle() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userA", "", 0L,
                true, "userA",
                false, true, false,
                30_000L, TIMEOUT, TIMEOUT, 100L);
        assertEquals(RecoveryDecision.Kind.SETTLE_THEN_RESULT, d.kind);
    }

    @Test
    public void awaitingPostBreak_continues() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userA", "", 0L,
                true, "userA",
                false, false, true,
                25 * 60_000L, 0L, TIMEOUT, 100L);
        assertEquals(RecoveryDecision.Kind.CONTINUE_ACTIVE, d.kind);
    }

    @Test
    public void lastResult_breakStarted_shows() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userA", "break_started", 42L,
                false, "",
                false, false, false,
                0L, 0L, TIMEOUT, 0L);
        assertEquals(RecoveryDecision.Kind.SHOW_RESULT, d.kind);
        assertEquals(RecoveryDecision.ResultKind.BREAK_STARTED, d.resultKind);
        assertEquals(42L, d.sessionId);
    }

    @Test
    public void lastResult_takesPrecedenceOverCheckpoint() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userA", "failed_timeout", 7L,
                true, "userA",
                true, false, false,
                60_000L, 0L, TIMEOUT, 100L);
        assertEquals(RecoveryDecision.Kind.SHOW_RESULT, d.kind);
        assertEquals(RecoveryDecision.ResultKind.FAILED_TIMEOUT, d.resultKind);
    }

    @Test
    public void noCheckpointNoResult_none() {
        RecoveryDecision d = SessionRecoveryPolicy.decide(
                "userA", "", 0L,
                false, "",
                false, false, false,
                0L, 0L, TIMEOUT, 0L);
        assertEquals(RecoveryDecision.Kind.NONE, d.kind);
    }
}
