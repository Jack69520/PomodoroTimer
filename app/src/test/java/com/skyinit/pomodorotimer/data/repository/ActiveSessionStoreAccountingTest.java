package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ActiveSessionStoreAccountingTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        ActiveSessionStore.clear(context);
    }

    @Test
    public void saveLoad_roundTripsTotalPausedElapsed() {
        long now = SystemClock.elapsedRealtime();
        ActiveSessionStore.save(
                context, "u1", 1L, 1, now + 60_000L, 60_000L,
                false, true, 0, System.currentTimeMillis(),
                1, now - 5_000L, 25 * 60_000L, 12_000L,
                -1, -1, "", "学习", "", "", false, false);

        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(context);
        assertEquals(12_000L, cp.totalPausedElapsedMs);
        assertEquals(25 * 60_000L, cp.plannedDurationMs);
    }

    @Test
    public void load_missingTotalPausedKey_defaultsZero() {
        long now = SystemClock.elapsedRealtime();
        ActiveSessionStore.save(
                context, "u1", 1L, 1, now + 60_000L, 60_000L,
                true, false, 0, System.currentTimeMillis(),
                0, 0L, 25 * 60_000L, 0L,
                -1, -1, "", "学习", "", "", false, false);
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(context);
        assertEquals(0L, cp.totalPausedElapsedMs);
    }

    @Test
    public void computeElapsed_paused_doesNotGrowWithWallClock() {
        long now = SystemClock.elapsedRealtime();
        ActiveSessionStore.save(
                context, "u1", 1L, 1, now + 60_000L, 20 * 60_000L,
                false, true, 0, System.currentTimeMillis() - 5 * 60_000L,
                1, now, 25 * 60_000L, 0L,
                -1, -1, "", "学习", "", "", false, false);
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(context);
        long elapsedBefore = ActiveSessionStore.computeElapsedMillis(cp);
        ShadowSystemClock.advanceBy(10, TimeUnit.MINUTES);
        ActiveSessionStore.Checkpoint cpAfter = ActiveSessionStore.load(context);
        long elapsedAfter = ActiveSessionStore.computeElapsedMillis(cpAfter);
        assertEquals(elapsedBefore, elapsedAfter);
        assertEquals(5 * 60_000L, elapsedAfter);
    }

    @Test
    public void saveEligible_requiresActiveFocus_notPauseWall() {
        long now = SystemClock.elapsedRealtime();
        // planned 25m, remaining 24m30s → active 30s < 60s SAVE_ELIGIBLE
        ActiveSessionStore.save(
                context, "u1", 1L, 1, now + 60_000L, 24 * 60_000L + 30_000L,
                false, true, 0, System.currentTimeMillis() - 30 * 60_000L,
                1, now - 30 * 60_000L, 25 * 60_000L, 0L,
                -1, -1, "", "学习", "", "", false, false);
        assertFalse(ActiveSessionStore.isSaveEligible(ActiveSessionStore.load(context)));

        // planned 25m, remaining 24m → active 60s >= SAVE_ELIGIBLE
        ActiveSessionStore.save(
                context, "u1", 1L, 1, now + 60_000L, 24 * 60_000L,
                false, true, 0, System.currentTimeMillis(),
                1, now, 25 * 60_000L, 0L,
                -1, -1, "", "学习", "", "", false, false);
        assertTrue(ActiveSessionStore.isSaveEligible(ActiveSessionStore.load(context)));
    }
}
