package com.skyinit.pomodorotimer.ui.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.domain.timer.SessionSettlePolicy;
import com.skyinit.pomodorotimer.domain.timer.SettleOutcome;

/**
 * 结束本轮确认文案 / 结算结果 Toast 映射（与 {@link SessionSettlePolicy} 对齐）。
 */
public final class SessionStopUiHelper {

    private SessionStopUiHelper() {
    }

    @StringRes
    public static int confirmMessageRes(@NonNull SettleOutcome preview) {
        switch (preview) {
            case SAVE_EARLY_END:
                return R.string.timer_confirm_reset_message_save;
            case DISCARD_SHORT:
                return R.string.timer_confirm_reset_message_short;
            case DISCARD_STRICT:
                return R.string.timer_confirm_reset_message_strict;
            default:
                return R.string.timer_confirm_reset_message;
        }
    }

    @NonNull
    public static String toastForSettleResult(@NonNull Context context,
                                             @NonNull String resultCode,
                                             long activeDurationMs) {
        if (ActiveSessionStore.RESULT_EARLY_ENDED.equals(resultCode)) {
            int minutes = (int) Math.max(1L, (activeDurationMs + 59_999L) / 60_000L);
            return context.getString(R.string.timer_toast_early_ended_saved, minutes);
        }
        if (ActiveSessionStore.RESULT_DISCARDED_SHORT.equals(resultCode)) {
            return context.getString(R.string.timer_toast_discard_under_60s);
        }
        if (ActiveSessionStore.RESULT_DISCARDED_STRICT.equals(resultCode)) {
            return context.getString(R.string.timer_toast_discard_strict);
        }
        return context.getString(R.string.timer_toast_timer_ended);
    }

    public static SettleOutcome previewUserAbort(boolean strictPomodoro, long activeDurationMs) {
        return SessionSettlePolicy.previewForConfirm(strictPomodoro, activeDurationMs, true);
    }
}
