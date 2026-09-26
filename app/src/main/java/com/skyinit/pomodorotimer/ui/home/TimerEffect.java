package com.skyinit.pomodorotimer.ui.home;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.domain.timer.PauseReasonPromptMode;
import com.skyinit.pomodorotimer.service.TimerService;

/**
 * 全屏计时页一次性副作用。
 */
public final class TimerEffect {

    public enum Type {
        DELIVER_ACTION,
        DELIVER_ACTION_WITH_REASON,
        SHOW_TOAST,
        SHOW_PAUSE_REASON_DIALOG,
        DISMISS_PAUSE_REASON_DIALOG,
        SHOW_STOP_CONFIRM,
        SHOW_END_BREAK_CONFIRM,
        SHOW_EXIT_CONFIRM,
        /** 投递 serviceAction 后 toast + 震动 + 回首页。 */
        FINISH_SESSION
    }

    public final Type type;
    @Nullable
    public final String serviceAction;
    @Nullable
    public final String pauseReason;
    public final PauseReasonPromptMode promptMode;
    @StringRes
    public final int toastRes;

    private TimerEffect(Type type,
                        @Nullable String serviceAction,
                        @Nullable String pauseReason,
                        PauseReasonPromptMode promptMode,
                        @StringRes int toastRes) {
        this.type = type;
        this.serviceAction = serviceAction;
        this.pauseReason = pauseReason;
        this.promptMode = promptMode;
        this.toastRes = toastRes;
    }

    public static TimerEffect deliver(String action) {
        return new TimerEffect(Type.DELIVER_ACTION, action, null, null, 0);
    }

    public static TimerEffect deliverSetReason(String reason) {
        return new TimerEffect(Type.DELIVER_ACTION_WITH_REASON,
                TimerService.ACTION_SET_PAUSE_REASON, reason, null, 0);
    }

    public static TimerEffect showToast(@StringRes int resId) {
        return new TimerEffect(Type.SHOW_TOAST, null, null, null, resId);
    }

    public static TimerEffect showPauseReasonDialog(PauseReasonPromptMode mode) {
        return new TimerEffect(Type.SHOW_PAUSE_REASON_DIALOG, null, null, mode, 0);
    }

    public static TimerEffect dismissPauseReasonDialog() {
        return new TimerEffect(Type.DISMISS_PAUSE_REASON_DIALOG, null, null, null, 0);
    }

    public static TimerEffect showStopConfirm() {
        return new TimerEffect(Type.SHOW_STOP_CONFIRM, null, null, null, 0);
    }

    public static TimerEffect showEndBreakConfirm() {
        return new TimerEffect(Type.SHOW_END_BREAK_CONFIRM, null, null, null, 0);
    }

    public static TimerEffect showExitConfirm() {
        return new TimerEffect(Type.SHOW_EXIT_CONFIRM, null, null, null, 0);
    }

    public static TimerEffect finishSession(String action, @StringRes int toastRes) {
        return new TimerEffect(Type.FINISH_SESSION, action, null, null, toastRes);
    }
}
