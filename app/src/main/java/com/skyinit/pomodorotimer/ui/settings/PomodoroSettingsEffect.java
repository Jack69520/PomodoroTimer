package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.util.LockScreenTimerGate;

/**
 * 番茄钟设置页一次性副作用。
 */
public final class PomodoroSettingsEffect {

    public enum Type {
        SHOW_TOAST,
        SHOW_STUDY_DURATION_PICKER,
        SHOW_SINGLE_CHOICE,
        SHOW_DND_PERMISSION_DIALOG,
        OPEN_DND_SETTINGS,
        SHOW_LOCK_SCREEN_PERMISSION_DIALOG,
        OPEN_FULL_SCREEN_INTENT_SETTINGS,
        OPEN_NOTIFICATION_SETTINGS
    }

    public enum ChoiceKind {
        BREAK_DURATION,
        PAUSE_COUNT,
        LONG_BREAK_INTERVAL,
        LONG_BREAK_DURATION
    }

    public final Type type;
    @StringRes
    public final int toastRes;
    public final long studyTimeMs;
    public final ChoiceKind choiceKind;
    public final int selectedIndex;
    @Nullable
    public final LockScreenTimerGate.EnablePrecondition lockScreenPrecondition;

    private PomodoroSettingsEffect(Type type,
                                   @StringRes int toastRes,
                                   long studyTimeMs,
                                   ChoiceKind choiceKind,
                                   int selectedIndex,
                                   @Nullable LockScreenTimerGate.EnablePrecondition lockScreenPrecondition) {
        this.type = type;
        this.toastRes = toastRes;
        this.studyTimeMs = studyTimeMs;
        this.choiceKind = choiceKind;
        this.selectedIndex = selectedIndex;
        this.lockScreenPrecondition = lockScreenPrecondition;
    }

    public static PomodoroSettingsEffect showToast(@StringRes int resId) {
        return new PomodoroSettingsEffect(Type.SHOW_TOAST, resId, 0, null, 0, null);
    }

    public static PomodoroSettingsEffect showStudyDurationPicker(long currentMs) {
        return new PomodoroSettingsEffect(Type.SHOW_STUDY_DURATION_PICKER, 0, currentMs, null, 0, null);
    }

    public static PomodoroSettingsEffect showSingleChoice(ChoiceKind kind, int selectedIndex) {
        return new PomodoroSettingsEffect(Type.SHOW_SINGLE_CHOICE, 0, 0, kind, selectedIndex, null);
    }

    public static PomodoroSettingsEffect showDndPermissionDialog() {
        return new PomodoroSettingsEffect(Type.SHOW_DND_PERMISSION_DIALOG, 0, 0, null, 0, null);
    }

    public static PomodoroSettingsEffect openDndSettings() {
        return new PomodoroSettingsEffect(Type.OPEN_DND_SETTINGS, 0, 0, null, 0, null);
    }

    public static PomodoroSettingsEffect showLockScreenPermissionDialog(
            @Nullable LockScreenTimerGate.EnablePrecondition precondition) {
        return new PomodoroSettingsEffect(
                Type.SHOW_LOCK_SCREEN_PERMISSION_DIALOG, 0, 0, null, 0, precondition);
    }

    public static PomodoroSettingsEffect openFullScreenIntentSettings() {
        return new PomodoroSettingsEffect(Type.OPEN_FULL_SCREEN_INTENT_SETTINGS, 0, 0, null, 0, null);
    }

    public static PomodoroSettingsEffect openNotificationSettings() {
        return new PomodoroSettingsEffect(Type.OPEN_NOTIFICATION_SETTINGS, 0, 0, null, 0, null);
    }
}
