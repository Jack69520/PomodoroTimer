package com.skyinit.pomodorotimer.ui.settings;

/**
 * 番茄钟设置页不可变 UI 快照。
 */
public final class PomodoroSettingsUiState {

    public final boolean ready;
    public final long studyTimeMs;
    public final String studyDurationLabel;
    public final long breakTimeMs;
    public final String breakDurationLabel;
    public final int maxPauseCount;
    public final String maxPauseLabel;
    public final int pauseReasonPromptMode;
    public final String pauseReasonPromptLabel;
    public final boolean autoStartAfterBreak;
    public final boolean longBreakEnabled;
    public final int pomodorosBeforeLongBreak;
    public final String longBreakIntervalLabel;
    public final int longBreakDurationMinutes;
    public final String longBreakDurationLabel;
    public final boolean dndEnabled;
    public final boolean dndHasAccess;
    public final boolean autoBlockDuringPomodoro;
    public final boolean lockScreenFullscreenEnabled;
    public final boolean autoDeleteCompleted;
    public final boolean collectionProgressDetails;

    public PomodoroSettingsUiState(boolean ready,
                                   long studyTimeMs,
                                   String studyDurationLabel,
                                   long breakTimeMs,
                                   String breakDurationLabel,
                                   int maxPauseCount,
                                   String maxPauseLabel,
                                   int pauseReasonPromptMode,
                                   String pauseReasonPromptLabel,
                                   boolean autoStartAfterBreak,
                                   boolean longBreakEnabled,
                                   int pomodorosBeforeLongBreak,
                                   String longBreakIntervalLabel,
                                   int longBreakDurationMinutes,
                                   String longBreakDurationLabel,
                                   boolean dndEnabled,
                                   boolean dndHasAccess,
                                   boolean autoBlockDuringPomodoro,
                                   boolean lockScreenFullscreenEnabled,
                                   boolean autoDeleteCompleted,
                                   boolean collectionProgressDetails) {
        this.ready = ready;
        this.studyTimeMs = studyTimeMs;
        this.studyDurationLabel = studyDurationLabel;
        this.breakTimeMs = breakTimeMs;
        this.breakDurationLabel = breakDurationLabel;
        this.maxPauseCount = maxPauseCount;
        this.maxPauseLabel = maxPauseLabel;
        this.pauseReasonPromptMode = pauseReasonPromptMode;
        this.pauseReasonPromptLabel = pauseReasonPromptLabel;
        this.autoStartAfterBreak = autoStartAfterBreak;
        this.longBreakEnabled = longBreakEnabled;
        this.pomodorosBeforeLongBreak = pomodorosBeforeLongBreak;
        this.longBreakIntervalLabel = longBreakIntervalLabel;
        this.longBreakDurationMinutes = longBreakDurationMinutes;
        this.longBreakDurationLabel = longBreakDurationLabel;
        this.dndEnabled = dndEnabled;
        this.dndHasAccess = dndHasAccess;
        this.autoBlockDuringPomodoro = autoBlockDuringPomodoro;
        this.lockScreenFullscreenEnabled = lockScreenFullscreenEnabled;
        this.autoDeleteCompleted = autoDeleteCompleted;
        this.collectionProgressDetails = collectionProgressDetails;
    }
}
