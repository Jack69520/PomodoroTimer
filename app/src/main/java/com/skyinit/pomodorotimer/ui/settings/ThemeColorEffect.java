package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.R;

/**
 * 主题色页一次性副作用。
 */
public final class ThemeColorEffect {

    public enum Type {
        THEME_APPLIED,
        THEME_FAILED
    }

    public final Type type;
    @Nullable
    public final String themeKey;
    @StringRes
    public final int toastRes;

    private ThemeColorEffect(Type type, @Nullable String themeKey, @StringRes int toastRes) {
        this.type = type;
        this.themeKey = themeKey;
        this.toastRes = toastRes;
    }

    @NonNull
    public static ThemeColorEffect themeApplied(@NonNull String themeKey) {
        return new ThemeColorEffect(Type.THEME_APPLIED, themeKey, R.string.settings_toast_theme_applied);
    }

    @NonNull
    public static ThemeColorEffect themeFailed() {
        return new ThemeColorEffect(Type.THEME_FAILED, null, R.string.settings_toast_theme_failed);
    }
}
