package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import com.skyinit.pomodorotimer.R;

/**
 * 账户切换前的计时会话防护。
 * <p>
 * Repository 层只依赖持久化的会话快照，不直接绑定 Service 或 Activity，保持 MVVM 分层边界。
 */
public final class AccountTimerStateGuard {

    private final Context appContext;

    public AccountTimerStateGuard(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public boolean hasActiveTimerState() {
        ActiveSessionStore.Checkpoint checkpoint = ActiveSessionStore.load(appContext);
        if (checkpoint == null) {
            return false;
        }
        return checkpoint.running || checkpoint.paused || checkpoint.awaitingPostBreakChoice;
    }

    public String getBlockedMessage() {
        return appContext.getString(R.string.account_error_active_timer_session);
    }
}
