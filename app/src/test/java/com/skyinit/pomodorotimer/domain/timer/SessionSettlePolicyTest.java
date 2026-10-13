package com.skyinit.pomodorotimer.domain.timer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionSettlePolicyTest {

    @Test
    public void naturalComplete_alwaysComplete() {
        assertEquals(SettleOutcome.COMPLETE,
                SessionSettlePolicy.decide(SessionEndReason.NATURAL_COMPLETE, true, 0L, true));
        assertEquals(SettleOutcome.COMPLETE,
                SessionSettlePolicy.decide(SessionEndReason.NATURAL_COMPLETE, false, 10L, true));
    }

    @Test
    public void userAbort_short_discards() {
        assertEquals(SettleOutcome.DISCARD_SHORT,
                SessionSettlePolicy.decide(SessionEndReason.USER_ABORT, false, 59_999L, true));
    }

    @Test
    public void userAbort_eligible_savesEarlyEnd() {
        assertEquals(SettleOutcome.SAVE_EARLY_END,
                SessionSettlePolicy.decide(
                        SessionEndReason.USER_ABORT, false, SessionSettlePolicy.SAVE_ELIGIBLE_MS, true));
    }

    @Test
    public void userAbort_strict_discards() {
        assertEquals(SettleOutcome.DISCARD_STRICT,
                SessionSettlePolicy.decide(SessionEndReason.USER_ABORT, true, 10 * 60_000L, true));
    }

    @Test
    public void pauseTimeout_counted_or_recordOnly() {
        assertEquals(SettleOutcome.SAVE_FAILED_COUNTED,
                SessionSettlePolicy.decide(SessionEndReason.PAUSE_TIMEOUT, false, 60_000L, true));
        assertEquals(SettleOutcome.SAVE_FAILED_RECORD_ONLY,
                SessionSettlePolicy.decide(SessionEndReason.PAUSE_TIMEOUT, true, 60_000L, true));
        assertEquals(SettleOutcome.DISCARD_SHORT,
                SessionSettlePolicy.decide(SessionEndReason.PAUSE_TIMEOUT, false, 1L, true));
    }

    @Test
    public void forcePaths_alwaysDiscard() {
        assertEquals(SettleOutcome.DISCARD_FORCE,
                SessionSettlePolicy.decide(SessionEndReason.ACCOUNT_SWITCH, false, 120_000L, true));
        assertEquals(SettleOutcome.DISCARD_FORCE,
                SessionSettlePolicy.decide(SessionEndReason.FORCE_FAIL, false, 120_000L, true));
    }

    @Test
    public void nonStudy_discards() {
        assertEquals(SettleOutcome.DISCARD_FORCE,
                SessionSettlePolicy.decide(SessionEndReason.USER_ABORT, false, 120_000L, false));
    }

    @Test
    public void preview_matchesDecide() {
        assertEquals(
                SessionSettlePolicy.decide(SessionEndReason.USER_ABORT, false, 90_000L, true),
                SessionSettlePolicy.previewForConfirm(false, 90_000L, true));
    }

    @Test
    public void outcomeFlags() {
        assertTrue(SettleOutcome.SAVE_EARLY_END.shouldPersist());
        assertTrue(SettleOutcome.SAVE_EARLY_END.countsTowardFocusDuration());
        assertFalse(SettleOutcome.SAVE_EARLY_END.isCompletedTomato());
        assertTrue(SettleOutcome.SAVE_FAILED_RECORD_ONLY.shouldPersist());
        assertFalse(SettleOutcome.SAVE_FAILED_RECORD_ONLY.countsTowardFocusDuration());
        assertFalse(SettleOutcome.DISCARD_SHORT.shouldPersist());
    }
}
