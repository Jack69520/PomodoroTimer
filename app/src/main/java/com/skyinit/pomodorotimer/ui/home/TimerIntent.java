package com.skyinit.pomodorotimer.ui.home;

import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.domain.timer.PauseReasonPromptMode;

/**
 * 全屏计时页用户意图。
 */
public final class TimerIntent {

    public enum Type {
        SERVICE_BOUND,
        SCREEN_RESUMED,
        PRIMARY_CLICKED,
        SECONDARY_CLICKED,
        REASON_PICKED,
        REASON_SKIPPED,
        REASON_QUICK_RESUME,
        DIALOG_DISMISSED_AS_SKIP,
        CONFIRM_STOP,
        CONFIRM_END_BREAK,
        CONFIRM_EXIT_RESET
    }

    public final Type type;
    @Nullable
    public final String reason;
    public final PauseReasonPromptMode promptMode;

    private TimerIntent(Type type, @Nullable String reason, PauseReasonPromptMode promptMode) {
        this.type = type;
        this.reason = reason;
        this.promptMode = promptMode;
    }

    public static TimerIntent serviceBound() {
        return new TimerIntent(Type.SERVICE_BOUND, null, null);
    }

    public static TimerIntent screenResumed() {
        return new TimerIntent(Type.SCREEN_RESUMED, null, null);
    }

    public static TimerIntent primaryClicked() {
        return new TimerIntent(Type.PRIMARY_CLICKED, null, null);
    }

    public static TimerIntent secondaryClicked() {
        return new TimerIntent(Type.SECONDARY_CLICKED, null, null);
    }

    public static TimerIntent reasonPicked(String reason) {
        return new TimerIntent(Type.REASON_PICKED, reason, null);
    }

    public static TimerIntent reasonSkipped() {
        return new TimerIntent(Type.REASON_SKIPPED, null, null);
    }

    public static TimerIntent reasonQuickResume() {
        return new TimerIntent(Type.REASON_QUICK_RESUME, null, null);
    }

    public static TimerIntent dialogDismissedAsSkip() {
        return new TimerIntent(Type.DIALOG_DISMISSED_AS_SKIP, null, null);
    }

    public static TimerIntent confirmStop() {
        return new TimerIntent(Type.CONFIRM_STOP, null, null);
    }

    public static TimerIntent confirmEndBreak() {
        return new TimerIntent(Type.CONFIRM_END_BREAK, null, null);
    }

    public static TimerIntent confirmExitReset() {
        return new TimerIntent(Type.CONFIRM_EXIT_RESET, null, null);
    }
}
