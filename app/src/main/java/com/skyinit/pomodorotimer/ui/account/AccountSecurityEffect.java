package com.skyinit.pomodorotimer.ui.account;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * 「账户与安全」页一次性副作用。
 */
public final class AccountSecurityEffect {

    public enum Type {
        SHOW_TOAST,
        SHOW_TOAST_TEXT,
        SHOW_LOGOUT_CONFIRM,
        SHOW_DELETE_CONFIRM,
        SHOW_GUARD_PROMPT,
        SHOW_DELETE_SUCCESS,
        START_ACTIVITY,
        FINISH
    }

    public final Type type;
    @StringRes
    public final int toastRes;
    @Nullable
    public final String toastText;
    @Nullable
    public final Class<? extends Activity> activityClass;

    private AccountSecurityEffect(Type type,
                                  @StringRes int toastRes,
                                  @Nullable String toastText,
                                  @Nullable Class<? extends Activity> activityClass) {
        this.type = type;
        this.toastRes = toastRes;
        this.toastText = toastText;
        this.activityClass = activityClass;
    }

    @NonNull
    public static AccountSecurityEffect showToast(@StringRes int resId) {
        return new AccountSecurityEffect(Type.SHOW_TOAST, resId, null, null);
    }

    @NonNull
    public static AccountSecurityEffect showToastText(@NonNull String message) {
        return new AccountSecurityEffect(Type.SHOW_TOAST_TEXT, 0, message, null);
    }

    @NonNull
    public static AccountSecurityEffect showLogoutConfirm() {
        return new AccountSecurityEffect(Type.SHOW_LOGOUT_CONFIRM, 0, null, null);
    }

    @NonNull
    public static AccountSecurityEffect showDeleteConfirm() {
        return new AccountSecurityEffect(Type.SHOW_DELETE_CONFIRM, 0, null, null);
    }

    @NonNull
    public static AccountSecurityEffect showGuardPrompt() {
        return new AccountSecurityEffect(Type.SHOW_GUARD_PROMPT, 0, null, null);
    }

    @NonNull
    public static AccountSecurityEffect showDeleteSuccess() {
        return new AccountSecurityEffect(Type.SHOW_DELETE_SUCCESS, 0, null, null);
    }

    @NonNull
    public static AccountSecurityEffect startActivity(@NonNull Class<? extends Activity> cls) {
        return new AccountSecurityEffect(Type.START_ACTIVITY, 0, null, cls);
    }

    @NonNull
    public static AccountSecurityEffect finish() {
        return new AccountSecurityEffect(Type.FINISH, 0, null, null);
    }

    @NonNull
    public static AccountSecurityEffect finishWithToast(@StringRes int resId) {
        return new AccountSecurityEffect(Type.FINISH, resId, null, null);
    }
}
