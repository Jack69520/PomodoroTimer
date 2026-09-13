package com.skyinit.pomodorotimer.ui.home.todoedit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * 任务编辑页一次性副作用。
 */
public final class TaskEditEffect {

    public enum Type {
        TOAST_RES,
        TOAST_TEXT,
        SAVE_SUCCESS,
        SAVE_FAILED
    }

    public final Type type;
    @StringRes
    public final int resId;
    @Nullable
    public final String text;

    private TaskEditEffect(Type type, @StringRes int resId, @Nullable String text) {
        this.type = type;
        this.resId = resId;
        this.text = text;
    }

    @NonNull
    public static TaskEditEffect toastRes(@StringRes int resId) {
        return new TaskEditEffect(Type.TOAST_RES, resId, null);
    }

    @NonNull
    public static TaskEditEffect toastText(@NonNull String text) {
        return new TaskEditEffect(Type.TOAST_TEXT, 0, text);
    }

    @NonNull
    public static TaskEditEffect saveSuccess() {
        return new TaskEditEffect(Type.SAVE_SUCCESS, 0, null);
    }

    @NonNull
    public static TaskEditEffect saveFailed(@Nullable String message) {
        return new TaskEditEffect(Type.SAVE_FAILED, 0, message);
    }
}
