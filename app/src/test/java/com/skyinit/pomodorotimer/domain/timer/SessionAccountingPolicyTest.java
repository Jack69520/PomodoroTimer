package com.skyinit.pomodorotimer.domain.timer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SessionAccountingPolicyTest {

    private static final long PLANNED_25M = 25L * 60L * 1000L;

    @Test
    public void settle_completeNoPause() {
        SessionAccountingResult r = SessionAccountingPolicy.settle(
                PLANNED_25M, 0L, 0L, 0L, false, 2_000_000L, 1_000_000L);
        assertEquals(PLANNED_25M, r.activeDurationMs);
        assertEquals(0L, r.pausedDurationMs);
        assertEquals(PLANNED_25M, r.plannedDurationMs);
        assertEquals(2_000_000L, r.endTimeMs);
    }

    @Test
    public void settle_completeWithAccumulatedPause() {
        long paused = 8L * 60L * 1000L;
        SessionAccountingResult r = SessionAccountingPolicy.settle(
                PLANNED_25M, 0L, paused, 99_999L, false, 3_000_000L, 1_000_000L);
        assertEquals(PLANNED_25M, r.activeDurationMs);
        assertEquals(paused, r.pausedDurationMs);
    }

    @Test
    public void settle_failWhilePaused_includesCurrentSegment() {
        long remaining = 10L * 60L * 1000L;
        long acc = 2L * 60L * 1000L;
        long cur = 5L * 60L * 1000L;
        SessionAccountingResult r = SessionAccountingPolicy.settle(
                PLANNED_25M, remaining, acc, cur, true, 3_000_000L, 1_000_000L);
        assertEquals(15L * 60L * 1000L, r.activeDurationMs);
        assertEquals(7L * 60L * 1000L, r.pausedDurationMs);
    }

    @Test
    public void settle_negativeRemaining_treatedAsZero() {
        SessionAccountingResult r = SessionAccountingPolicy.settle(
                PLANNED_25M, -1_000L, 0L, 0L, false, 1L, 1L);
        assertEquals(PLANNED_25M, r.activeDurationMs);
    }

    @Test
    public void settle_remainingGreaterThanPlanned_activeZero() {
        SessionAccountingResult r = SessionAccountingPolicy.settle(
                PLANNED_25M, PLANNED_25M + 1_000L, 0L, 0L, false, 1L, 1L);
        assertEquals(0L, r.activeDurationMs);
    }

    @Test
    public void settle_plannedNonPositive_activeZero() {
        SessionAccountingResult r = SessionAccountingPolicy.settle(
                0L, 5_000L, 1_000L, 0L, false, 10L, 1L);
        assertEquals(0L, r.activeDurationMs);
        assertEquals(1_000L, r.pausedDurationMs);
        assertTrue(r.activeDurationMs >= 0L);
    }

    @Test
    public void settle_notPaused_ignoresCurrentSegment() {
        SessionAccountingResult r = SessionAccountingPolicy.settle(
                PLANNED_25M, 0L, 3_000L, 9_000L, false, 1L, 1L);
        assertEquals(3_000L, r.pausedDurationMs);
    }

    @Test
    public void settle_endTimeNotBeforeStart() {
        SessionAccountingResult r = SessionAccountingPolicy.settle(
                PLANNED_25M, 0L, 0L, 0L, false, 500L, 1_000L);
        assertEquals(1_000L, r.endTimeMs);
    }

    @Test
    public void resolveHelpers_matchSettle() {
        assertEquals(10_000L,
                SessionAccountingPolicy.resolveActiveDurationMs(30_000L, 20_000L));
        assertEquals(12_000L,
                SessionAccountingPolicy.resolvePausedDurationMs(5_000L, 7_000L, true));
        assertEquals(5_000L,
                SessionAccountingPolicy.resolvePausedDurationMs(5_000L, 7_000L, false));
    }
}
