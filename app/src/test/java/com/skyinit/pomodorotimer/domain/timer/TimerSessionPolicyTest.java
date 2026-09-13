package com.skyinit.pomodorotimer.domain.timer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimerSessionPolicyTest {

    @Test
    public void decide_runningExpired_completes() {
        SettleDecision d = TimerSessionPolicy.decide(
                SessionPhase.RUNNING_STUDY, 0L, 0L,
                TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS, 1, 1);
        assertEquals(SettleDecision.COMPLETE_RUNNING, d);
    }

    @Test
    public void decide_staleGeneration_ignored() {
        SettleDecision d = TimerSessionPolicy.decide(
                SessionPhase.RUNNING_STUDY, 0L, 0L,
                TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS, 1, 2);
        assertEquals(SettleDecision.IGNORE, d);
    }

    @Test
    public void decide_pauseTimeout_fails() {
        SettleDecision d = TimerSessionPolicy.decide(
                SessionPhase.PAUSED, 60_000L,
                TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS,
                TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS, -1, 0);
        assertEquals(SettleDecision.FAIL_PAUSE_TIMEOUT, d);
    }

    @Test
    public void decide_runningNotDue_none() {
        SettleDecision d = TimerSessionPolicy.decide(
                SessionPhase.RUNNING_BREAK, 30_000L, 0L,
                TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS, -1, 0);
        assertEquals(SettleDecision.NONE, d);
    }

    @Test
    public void sessionClock_prefersWallWhenElapsedDiverges() {
        long nowElapsed = 1_000L;
        long endElapsed = nowElapsed + 8L * 60L * 60L * 1000L;
        long nowWall = 1_000_000L;
        long endWall = nowWall + 60_000L;
        assertTrue(SessionClock.shouldPreferWallClock(endElapsed, endWall, nowElapsed, nowWall));
        assertEquals(60_000L, SessionClock.computeRunningRemainingMs(
                endElapsed, endWall, nowElapsed, nowWall, true));
    }

    @Test
    public void sessionClock_usesElapsedWhenConsistent() {
        long nowElapsed = 10_000L;
        long endElapsed = 70_000L;
        long nowWall = 1_000_000L;
        long endWall = 1_060_000L;
        assertFalse(SessionClock.shouldPreferWallClock(endElapsed, endWall, nowElapsed, nowWall));
        assertEquals(60_000L, SessionClock.computeRunningRemainingMs(
                endElapsed, endWall, nowElapsed, nowWall, false));
    }

    @Test
    public void phase_fromFlags() {
        assertEquals(SessionPhase.PAUSED, SessionPhase.fromFlags(false, true, 0, false));
        assertEquals(SessionPhase.RUNNING_BREAK, SessionPhase.fromFlags(true, false, 1, false));
        assertEquals(SessionPhase.AWAITING_POST_BREAK, SessionPhase.fromFlags(false, false, 0, true));
    }
}
