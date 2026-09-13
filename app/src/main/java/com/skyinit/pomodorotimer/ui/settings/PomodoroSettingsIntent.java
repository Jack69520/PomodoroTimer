package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;

/**
 * 番茄钟设置页用户意图。
 */
public final class PomodoroSettingsIntent {

    public enum Type {
        REFRESH,
        SET_STUDY_DURATION,
        SET_BREAK_DURATION,
        SET_MAX_PAUSE,
        SET_AUTO_START,
        SET_LONG_BREAK_ENABLED,
        SET_LONG_BREAK_INTERVAL,
        SET_LONG_BREAK_DURATION,
        SET_DND,
        SET_AUTO_BLOCK,
        SET_AUTO_DELETE,
        SET_COLLECTION_PROGRESS_DETAILS,
        SET_LOCK_SCREEN_FULLSCREEN,
        OPEN_STUDY_DURATION_PICKER,
        OPEN_BREAK_DURATION_PICKER,
        OPEN_PAUSE_COUNT_PICKER,
        OPEN_LONG_BREAK_INTERVAL_PICKER,
        OPEN_LONG_BREAK_DURATION_PICKER,
        REQUEST_DND_PERMISSION,
        REQUEST_FULL_SCREEN_INTENT_PERMISSION,
        REQUEST_NOTIFICATION_PERMISSION_SETTINGS,
        CONFIRM_ENABLE_LOCK_SCREEN_DEGRADED
    }

    public final Type type;
    public final long longValue;
    public final int intValue;
    public final boolean boolValue;

    private PomodoroSettingsIntent(Type type, long longValue, int intValue, boolean boolValue) {
        this.type = type;
        this.longValue = longValue;
        this.intValue = intValue;
        this.boolValue = boolValue;
    }

    @NonNull
    public static PomodoroSettingsIntent refresh() {
        return new PomodoroSettingsIntent(Type.REFRESH, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent setStudyDuration(long millis) {
        return new PomodoroSettingsIntent(Type.SET_STUDY_DURATION, millis, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent setBreakDuration(long millis) {
        return new PomodoroSettingsIntent(Type.SET_BREAK_DURATION, millis, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent setMaxPause(int count) {
        return new PomodoroSettingsIntent(Type.SET_MAX_PAUSE, 0, count, false);
    }

    @NonNull
    public static PomodoroSettingsIntent setAutoStart(boolean enabled) {
        return new PomodoroSettingsIntent(Type.SET_AUTO_START, 0, 0, enabled);
    }

    @NonNull
    public static PomodoroSettingsIntent setLongBreakEnabled(boolean enabled) {
        return new PomodoroSettingsIntent(Type.SET_LONG_BREAK_ENABLED, 0, 0, enabled);
    }

    @NonNull
    public static PomodoroSettingsIntent setLongBreakInterval(int count) {
        return new PomodoroSettingsIntent(Type.SET_LONG_BREAK_INTERVAL, 0, count, false);
    }

    @NonNull
    public static PomodoroSettingsIntent setLongBreakDurationMinutes(int minutes) {
        return new PomodoroSettingsIntent(Type.SET_LONG_BREAK_DURATION, 0, minutes, false);
    }

    @NonNull
    public static PomodoroSettingsIntent setDnd(boolean enabled) {
        return new PomodoroSettingsIntent(Type.SET_DND, 0, 0, enabled);
    }

    @NonNull
    public static PomodoroSettingsIntent setAutoBlock(boolean enabled) {
        return new PomodoroSettingsIntent(Type.SET_AUTO_BLOCK, 0, 0, enabled);
    }

    @NonNull
    public static PomodoroSettingsIntent setAutoDelete(boolean enabled) {
        return new PomodoroSettingsIntent(Type.SET_AUTO_DELETE, 0, 0, enabled);
    }

    @NonNull
    public static PomodoroSettingsIntent setCollectionProgressDetails(boolean enabled) {
        return new PomodoroSettingsIntent(Type.SET_COLLECTION_PROGRESS_DETAILS, 0, 0, enabled);
    }

    @NonNull
    public static PomodoroSettingsIntent setLockScreenFullscreen(boolean enabled) {
        return new PomodoroSettingsIntent(Type.SET_LOCK_SCREEN_FULLSCREEN, 0, 0, enabled);
    }

    @NonNull
    public static PomodoroSettingsIntent openStudyDurationPicker() {
        return new PomodoroSettingsIntent(Type.OPEN_STUDY_DURATION_PICKER, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent openBreakDurationPicker() {
        return new PomodoroSettingsIntent(Type.OPEN_BREAK_DURATION_PICKER, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent openPauseCountPicker() {
        return new PomodoroSettingsIntent(Type.OPEN_PAUSE_COUNT_PICKER, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent openLongBreakIntervalPicker() {
        return new PomodoroSettingsIntent(Type.OPEN_LONG_BREAK_INTERVAL_PICKER, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent openLongBreakDurationPicker() {
        return new PomodoroSettingsIntent(Type.OPEN_LONG_BREAK_DURATION_PICKER, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent requestDndPermission() {
        return new PomodoroSettingsIntent(Type.REQUEST_DND_PERMISSION, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent requestFullScreenIntentPermission() {
        return new PomodoroSettingsIntent(Type.REQUEST_FULL_SCREEN_INTENT_PERMISSION, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent requestNotificationPermissionSettings() {
        return new PomodoroSettingsIntent(Type.REQUEST_NOTIFICATION_PERMISSION_SETTINGS, 0, 0, false);
    }

    @NonNull
    public static PomodoroSettingsIntent confirmEnableLockScreenDegraded() {
        return new PomodoroSettingsIntent(Type.CONFIRM_ENABLE_LOCK_SCREEN_DEGRADED, 0, 0, false);
    }
}
