package com.skyinit.pomodorotimer.ui.home;

import android.content.Context;

import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.domain.timer.PauseReasonPolicy;

/**
 * 计时页派生展示状态。
 */
public final class TimerScreenUiState {

    public final boolean pauseHintVisible;
    public final boolean pauseHintUrgent;
    @Nullable
    public final String pauseHintText;
    public final boolean paused;

    public TimerScreenUiState(boolean pauseHintVisible,
                              boolean pauseHintUrgent,
                              @Nullable String pauseHintText,
                              boolean paused) {
        this.pauseHintVisible = pauseHintVisible;
        this.pauseHintUrgent = pauseHintUrgent;
        this.pauseHintText = pauseHintText;
        this.paused = paused;
    }

    public static TimerScreenUiState from(@Nullable TimerUiState state,
                                          TimeFormatter formatter,
                                          Context context) {
        if (state == null || !state.paused) {
            return new TimerScreenUiState(false, false, null, false);
        }
        boolean urgent = PauseReasonPolicy.isPauseHintUrgent(state.pauseTimeoutRemainingMs);
        String countdown = formatter.format(state.pauseTimeoutRemainingMs);
        String text = urgent
                ? context.getString(R.string.timer_pause_fail_countdown_urgent, countdown)
                : context.getString(R.string.timer_pause_fail_countdown, countdown);
        return new TimerScreenUiState(true, urgent, text, true);
    }

    public interface TimeFormatter {
        String format(long millis);
    }
}
