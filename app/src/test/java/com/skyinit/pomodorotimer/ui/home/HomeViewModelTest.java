package com.skyinit.pomodorotimer.ui.home;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HomeViewModelTest {

    private static final long DEFAULT_MS = 25L * 60L * 1000L;

    @Test
    public void formatTimerText_default25Minutes_showsMmSs() {
        assertEquals("25:00", HomeViewModel.formatTimerText(DEFAULT_MS, 0, DEFAULT_MS));
    }

    @Test
    public void formatTimerText_oneHourDefault_usesHmsForStudySession() {
        long oneHour = 60L * 60L * 1000L;
        assertEquals("01:00:00", HomeViewModel.formatTimerText(oneHour, 0, oneHour));
    }

    @Test
    public void formatTimerText_breakSession_usesMmSsEvenForLongDefault() {
        long oneHour = 60L * 60L * 1000L;
        assertEquals("05:00", HomeViewModel.formatTimerText(5L * 60L * 1000L, 1, oneHour));
    }
}
