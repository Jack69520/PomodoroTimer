package com.skyinit.pomodorotimer.ui.account;

/**
 * 「账户与安全」页不可变 UI 快照。
 */
public final class AccountSecurityUiState {

    public final boolean registered;
    public final boolean actionInProgress;

    public AccountSecurityUiState(boolean registered, boolean actionInProgress) {
        this.registered = registered;
        this.actionInProgress = actionInProgress;
    }

    public AccountSecurityUiState withActionInProgress(boolean inProgress) {
        return new AccountSecurityUiState(registered, inProgress);
    }
}
