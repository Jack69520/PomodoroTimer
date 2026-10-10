package com.skyinit.pomodorotimer.domain.blocking;

import android.content.Context;

import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class BlockingRecoveryPolicyTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        ActiveSessionStore.clear(context);
    }

    @Test
    public void rejects_whenNoUsageStats() {
        saveStudyRunning("u1", false);
        assertFalse(BlockingRecoveryPolicy.shouldAttachTimerSource(
                ActiveSessionStore.load(context), "u1", true, false, false, false));
    }

    @Test
    public void accepts_autoBlock_studyRunning() {
        saveStudyRunning("u1", false);
        assertTrue(BlockingRecoveryPolicy.shouldAttachTimerSource(
                ActiveSessionStore.load(context), "u1", true, false, false, true));
    }

    @Test
    public void accepts_standalonePrefs_evenWithoutAutoBlock() {
        saveStudyRunning("u1", false);
        assertTrue(BlockingRecoveryPolicy.shouldAttachTimerSource(
                ActiveSessionStore.load(context), "u1", false, false, true, true));
    }

    @Test
    public void rejects_paused() {
        saveStudyRunning("u1", true);
        assertFalse(BlockingRecoveryPolicy.shouldAttachTimerSource(
                ActiveSessionStore.load(context), "u1", true, false, false, true));
    }

    @Test
    public void rejects_wrongUser() {
        saveStudyRunning("u1", false);
        assertFalse(BlockingRecoveryPolicy.shouldAttachTimerSource(
                ActiveSessionStore.load(context), "u2", true, false, false, true));
    }

    @Test
    public void rejects_whenNoReasonToAttach() {
        saveStudyRunning("u1", false);
        assertFalse(BlockingRecoveryPolicy.shouldAttachTimerSource(
                ActiveSessionStore.load(context), "u1", false, false, false, true));
    }

    private void saveStudyRunning(String userId, boolean paused) {
        long start = 1_700_000_000_000L;
        ActiveSessionStore.save(
                context,
                userId,
                start,
                1,
                0L,
                60_000L,
                true,
                paused,
                0,
                start,
                0,
                0L,
                25 * 60_000L,
                0L,
                -1,
                -1,
                "",
                "",
                "",
                "",
                false,
                false);
    }
}
