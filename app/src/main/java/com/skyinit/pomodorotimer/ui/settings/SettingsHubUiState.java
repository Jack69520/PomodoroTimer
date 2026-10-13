package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;

/**
 * 设置主页不可变 UI 快照。
 */
public final class SettingsHubUiState {

    @NonNull
    public final String themeName;
    public final int themeResId;
    public final boolean themeGradient;
    @NonNull
    public final String ringtoneLabel;
    /** 0 默认 / 1 静音 / 2 自定义。 */
    public final int ringtoneOption;
    public final boolean ringtoneDegraded;
    @NonNull
    public final String ringtoneHint;
    @NonNull
    public final String pomodoroSummary;

    public SettingsHubUiState(@NonNull String themeName,
                              int themeResId,
                              boolean themeGradient,
                              @NonNull String ringtoneLabel,
                              int ringtoneOption,
                              boolean ringtoneDegraded,
                              @NonNull String ringtoneHint,
                              @NonNull String pomodoroSummary) {
        this.themeName = themeName;
        this.themeResId = themeResId;
        this.themeGradient = themeGradient;
        this.ringtoneLabel = ringtoneLabel;
        this.ringtoneOption = ringtoneOption;
        this.ringtoneDegraded = ringtoneDegraded;
        this.ringtoneHint = ringtoneHint;
        this.pomodoroSummary = pomodoroSummary;
    }
}
