package com.skyinit.pomodorotimer.ui.account;

import androidx.annotation.NonNull;

/**
 * 「账户与安全」页用户意图。
 */
public final class AccountSecurityIntent {

    public enum Type {
        CHANGE_PASSWORD,
        LOGOUT,
        CONFIRM_LOGOUT,
        SWITCH_ACCOUNT,
        DELETE_ACCOUNT,
        CONFIRM_DELETE,
        CONTINUE_AFTER_DISABLE_BLOCKING,
        LOGIN,
        UPGRADE_REGISTER
    }

    public final Type type;

    private AccountSecurityIntent(Type type) {
        this.type = type;
    }

    @NonNull
    public static AccountSecurityIntent changePassword() {
        return new AccountSecurityIntent(Type.CHANGE_PASSWORD);
    }

    @NonNull
    public static AccountSecurityIntent logout() {
        return new AccountSecurityIntent(Type.LOGOUT);
    }

    @NonNull
    public static AccountSecurityIntent confirmLogout() {
        return new AccountSecurityIntent(Type.CONFIRM_LOGOUT);
    }

    @NonNull
    public static AccountSecurityIntent switchAccount() {
        return new AccountSecurityIntent(Type.SWITCH_ACCOUNT);
    }

    @NonNull
    public static AccountSecurityIntent deleteAccount() {
        return new AccountSecurityIntent(Type.DELETE_ACCOUNT);
    }

    @NonNull
    public static AccountSecurityIntent confirmDelete() {
        return new AccountSecurityIntent(Type.CONFIRM_DELETE);
    }

    @NonNull
    public static AccountSecurityIntent continueAfterDisablingBlocking() {
        return new AccountSecurityIntent(Type.CONTINUE_AFTER_DISABLE_BLOCKING);
    }

    @NonNull
    public static AccountSecurityIntent login() {
        return new AccountSecurityIntent(Type.LOGIN);
    }

    @NonNull
    public static AccountSecurityIntent upgradeRegister() {
        return new AccountSecurityIntent(Type.UPGRADE_REGISTER);
    }
}
