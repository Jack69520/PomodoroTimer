package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;

/**
 * 设置主页用户意图。
 */
public final class SettingsHubIntent {

    public enum Type {
        REFRESH,
        OPEN_ACCOUNT_PROFILE,
        OPEN_ACCOUNT_SECURITY,
        OPEN_THEME,
        OPEN_RINGTONE,
        OPEN_POMODORO,
        OPEN_SYSTEM_PERMISSIONS,
        RINGTONE_OPTION_SELECTED,
        CUSTOM_RINGTONE_PICKED,
        CUSTOM_RINGTONE_CANCELLED
    }

    public final Type type;
    /** 0 默认 / 1 静音 / 2 自定义。 */
    public final int ringtoneOption;
    public final String ringtoneUri;

    private SettingsHubIntent(Type type, int ringtoneOption, String ringtoneUri) {
        this.type = type;
        this.ringtoneOption = ringtoneOption;
        this.ringtoneUri = ringtoneUri;
    }

    @NonNull
    public static SettingsHubIntent refresh() {
        return new SettingsHubIntent(Type.REFRESH, 0, null);
    }

    @NonNull
    public static SettingsHubIntent openAccountProfile() {
        return new SettingsHubIntent(Type.OPEN_ACCOUNT_PROFILE, 0, null);
    }

    @NonNull
    public static SettingsHubIntent openAccountSecurity() {
        return new SettingsHubIntent(Type.OPEN_ACCOUNT_SECURITY, 0, null);
    }

    @NonNull
    public static SettingsHubIntent openTheme() {
        return new SettingsHubIntent(Type.OPEN_THEME, 0, null);
    }

    @NonNull
    public static SettingsHubIntent openRingtone() {
        return new SettingsHubIntent(Type.OPEN_RINGTONE, 0, null);
    }

    @NonNull
    public static SettingsHubIntent openPomodoro() {
        return new SettingsHubIntent(Type.OPEN_POMODORO, 0, null);
    }

    @NonNull
    public static SettingsHubIntent openSystemPermissions() {
        return new SettingsHubIntent(Type.OPEN_SYSTEM_PERMISSIONS, 0, null);
    }

    @NonNull
    public static SettingsHubIntent ringtoneOptionSelected(int option) {
        return new SettingsHubIntent(Type.RINGTONE_OPTION_SELECTED, option, null);
    }

    @NonNull
    public static SettingsHubIntent customRingtonePicked(@NonNull String uri) {
        return new SettingsHubIntent(Type.CUSTOM_RINGTONE_PICKED, 2, uri);
    }

    @NonNull
    public static SettingsHubIntent customRingtoneCancelled() {
        return new SettingsHubIntent(Type.CUSTOM_RINGTONE_CANCELLED, 0, null);
    }
}
