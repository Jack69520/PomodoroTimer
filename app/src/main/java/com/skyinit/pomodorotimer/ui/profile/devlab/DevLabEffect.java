package com.skyinit.pomodorotimer.ui.profile.devlab;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.R;

/**
 * 开发实验室一次性副作用。
 */
public final class DevLabEffect {

    public enum Type {
        TOAST,
        SHOW_CLEAR_CONFIRM
    }

    public final Type type;
    @StringRes
    public final int messageRes;

    private DevLabEffect(Type type, @StringRes int messageRes) {
        this.type = type;
        this.messageRes = messageRes;
    }

    @NonNull
    public static DevLabEffect toast(@StringRes int messageRes) {
        return new DevLabEffect(Type.TOAST, messageRes);
    }

    @NonNull
    public static DevLabEffect showClearConfirm() {
        return new DevLabEffect(Type.SHOW_CLEAR_CONFIRM, R.string.dev_lab_clear_confirm_message);
    }
}
