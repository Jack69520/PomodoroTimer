package com.skyinit.pomodorotimer.domain.timer;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PauseReasonPolicyTest {

    @Test
    public void appendUnsettledSlot_alignsWithPauseCount() {
        List<String> after = PauseReasonPolicy.appendUnsettledSlot(Collections.emptyList(), 1);
        assertEquals(1, after.size());
        assertEquals(PauseReasonPolicy.UNSETTLED, after.get(0));
        assertFalse(PauseReasonPolicy.isCurrentReasonSettled(after, 1));
    }

    @Test
    public void settleCurrentReason_writesLastSlotWithoutGrowingPastCount() {
        List<String> slots = PauseReasonPolicy.appendUnsettledSlot(Collections.emptyList(), 1);
        slots = PauseReasonPolicy.settleCurrentReason(slots, 1, "被打断");
        assertTrue(PauseReasonPolicy.isCurrentReasonSettled(slots, 1));
        assertEquals("被打断", slots.get(0));

        slots = PauseReasonPolicy.appendUnsettledSlot(slots, 2);
        assertEquals(2, slots.size());
        assertFalse(PauseReasonPolicy.isCurrentReasonSettled(slots, 2));
        slots = PauseReasonPolicy.settleCurrentReason(slots, 2, "临时有事");
        assertEquals(Arrays.asList("被打断", "临时有事"), slots);
    }

    @Test
    public void settleCurrentReason_overwritesWithoutDoubleCount() {
        List<String> slots = PauseReasonPolicy.appendUnsettledSlot(Collections.emptyList(), 1);
        slots = PauseReasonPolicy.settleCurrentReason(slots, 1, "其他");
        slots = PauseReasonPolicy.settleCurrentReason(slots, 1, "快速恢复");
        assertEquals(1, slots.size());
        assertEquals("快速恢复", slots.get(0));
    }

    @Test
    public void normalizeForPersist_fillsUnsettled() {
        List<String> slots = Arrays.asList("", "被打断");
        List<String> normalized = PauseReasonPolicy.normalizeForPersist(slots, 2, "未填写");
        assertEquals(Arrays.asList("未填写", "被打断"), normalized);
    }

    @Test
    public void shouldAutoPrompt_respectsModeAndSettled() {
        assertTrue(PauseReasonPolicy.shouldAutoPrompt(
                PauseReasonPromptMode.ASK_SKIPPABLE, true, false));
        assertTrue(PauseReasonPolicy.shouldAutoPrompt(
                PauseReasonPromptMode.REQUIRED, true, false));
        assertFalse(PauseReasonPolicy.shouldAutoPrompt(
                PauseReasonPromptMode.OFF, true, false));
        assertFalse(PauseReasonPolicy.shouldAutoPrompt(
                PauseReasonPromptMode.ASK_SKIPPABLE, false, false));
        assertFalse(PauseReasonPolicy.shouldAutoPrompt(
                PauseReasonPromptMode.ASK_SKIPPABLE, true, true));
    }

    @Test
    public void fromStorage_fallsBackToDefault() {
        assertEquals(PauseReasonPromptMode.ASK_SKIPPABLE, PauseReasonPromptMode.fromStorage(-1));
        assertEquals(PauseReasonPromptMode.REQUIRED, PauseReasonPromptMode.fromStorage(1));
        assertEquals(PauseReasonPromptMode.OFF, PauseReasonPromptMode.fromStorage(2));
    }

    @Test
    public void isPauseHintUrgent_underOneMinute() {
        assertTrue(PauseReasonPolicy.isPauseHintUrgent(59_000L));
        assertTrue(PauseReasonPolicy.isPauseHintUrgent(60_000L));
        assertFalse(PauseReasonPolicy.isPauseHintUrgent(60_001L));
        assertFalse(PauseReasonPolicy.isPauseHintUrgent(0L));
    }
}
