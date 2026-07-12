package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.TimerUiState;

/**
 * 账户切换前的计时会话防护。
 * <p>
 * 同时检查内存中的计时态与磁盘快照，与 {@link AccountOperationGuard} 保持一致。
 */
public final class AccountTimerStateGuard {

    private final Context appContext;
    private TimerStateRepository timerStateRepository;

    public AccountTimerStateGuard(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void setTimerStateRepository(TimerStateRepository timerStateRepository) {
        this.timerStateRepository = timerStateRepository;
    }

    public boolean hasActiveTimerState() {
        if (hasLiveTimerState()) {
            return true;
        }
        ActiveSessionStore.Checkpoint checkpoint = ActiveSessionStore.load(appContext);
        if (checkpoint == null) {
            return false;
        }
        return checkpoint.running || checkpoint.paused || checkpoint.awaitingPostBreakChoice;
    }

    private boolean hasLiveTimerState() {
        if (timerStateRepository == null) {
            return false;
        }
        TimerUiState state = timerStateRepository.getCurrentState();
        return state != null
                && (state.running || state.paused || state.awaitingPostBreakChoice);
    }

    public String getBlockedMessage() {
        return appContext.getString(R.string.account_error_active_timer_session);
    }
}
