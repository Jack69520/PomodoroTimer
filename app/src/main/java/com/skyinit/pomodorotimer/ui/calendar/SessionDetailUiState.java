package com.skyinit.pomodorotimer.ui.calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 记录详情页不可变 UI 状态。
 */
public final class SessionDetailUiState {

    public enum CompletionStatus {
        COMPLETED,
        FAILED,
        INCOMPLETE
    }

    public final boolean loading;
    public final boolean saving;
    public final boolean hasData;
    public final long startTime;
    public final long endTime;
    public final long durationMs;
    @Nullable
    public final String category;
    @NonNull
    public final CompletionStatus completionStatus;
    public final boolean hasPause;
    public final int pauseCount;
    @NonNull
    public final List<String> pauseReasons;
    public final boolean earlyEnd;
    public final int blockEventCount;
    @NonNull
    public final String blockEncouragement;
    @NonNull
    public final String notes;
    public final boolean canOpenBlockRecords;

    private SessionDetailUiState(boolean loading,
                                 boolean saving,
                                 boolean hasData,
                                 long startTime,
                                 long endTime,
                                 long durationMs,
                                 @Nullable String category,
                                 @NonNull CompletionStatus completionStatus,
                                 boolean hasPause,
                                 int pauseCount,
                                 @NonNull List<String> pauseReasons,
                                 boolean earlyEnd,
                                 int blockEventCount,
                                 @NonNull String blockEncouragement,
                                 @NonNull String notes,
                                 boolean canOpenBlockRecords) {
        this.loading = loading;
        this.saving = saving;
        this.hasData = hasData;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.category = category;
        this.completionStatus = completionStatus;
        this.hasPause = hasPause;
        this.pauseCount = pauseCount;
        this.pauseReasons = pauseReasons;
        this.earlyEnd = earlyEnd;
        this.blockEventCount = blockEventCount;
        this.blockEncouragement = blockEncouragement;
        this.notes = notes;
        this.canOpenBlockRecords = canOpenBlockRecords;
    }

    @NonNull
    public static SessionDetailUiState loading() {
        return new SessionDetailUiState(
                true, false, false,
                0L, 0L, 0L, null,
                CompletionStatus.INCOMPLETE,
                false, 0, Collections.emptyList(),
                false, 0, "", "", false);
    }

    @NonNull
    public SessionDetailUiState withSaving(boolean saving) {
        return new SessionDetailUiState(
                loading, saving, hasData,
                startTime, endTime, durationMs, category,
                completionStatus, hasPause, pauseCount, pauseReasons,
                earlyEnd, blockEventCount, blockEncouragement, notes, canOpenBlockRecords);
    }

    @NonNull
    public static SessionDetailUiState of(long startTime,
                                         long endTime,
                                         long durationMs,
                                         @Nullable String category,
                                         @NonNull CompletionStatus completionStatus,
                                         boolean hasPause,
                                         int pauseCount,
                                         @NonNull List<String> pauseReasons,
                                         boolean earlyEnd,
                                         int blockEventCount,
                                         @NonNull String blockEncouragement,
                                         @Nullable String notes,
                                         boolean saving) {
        List<String> safeReasons = pauseReasons != null
                ? Collections.unmodifiableList(pauseReasons)
                : Collections.emptyList();
        String safeNotes = notes != null ? notes : "";
        return new SessionDetailUiState(
                false,
                saving,
                true,
                startTime,
                endTime,
                durationMs,
                category,
                completionStatus,
                hasPause,
                Math.max(0, pauseCount),
                safeReasons,
                earlyEnd,
                Math.max(0, blockEventCount),
                blockEncouragement != null ? blockEncouragement : "",
                safeNotes,
                startTime > 0L);
    }
}
