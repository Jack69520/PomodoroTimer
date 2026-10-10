package com.skyinit.pomodorotimer.domain.blocking;

import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;

/**
 * Timer-linked blocking recovery policy shared by TimerService and sticky null Intent.
 * Never starts standalone.
 */
public final class BlockingRecoveryPolicy {

    private BlockingRecoveryPolicy() {
    }

    public static boolean shouldAttachTimerSource(
            @Nullable ActiveSessionStore.Checkpoint checkpoint,
            @Nullable String currentUserId,
            boolean autoBlockEnabled,
            boolean monitoringAlreadyOn,
            boolean standalonePrefsOn,
            boolean hasUsageStats) {
        if (!hasUsageStats) {
            return false;
        }
        if (currentUserId == null || currentUserId.isEmpty()) {
            return false;
        }
        if (checkpoint == null
                || !checkpoint.belongsToUser(currentUserId)
                || checkpoint.sessionType != 0
                || !checkpoint.running
                || checkpoint.paused
                || checkpoint.sessionStartTime <= 0L) {
            return false;
        }
        if (!autoBlockEnabled && !monitoringAlreadyOn && !standalonePrefsOn) {
            return false;
        }
        return true;
    }
}
