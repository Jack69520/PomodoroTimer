package com.skyinit.pomodorotimer.ui.settings;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;

/**
 * 设置主页一次性副作用。
 */
public final class SettingsHubEffect {

    public enum Type {
        SHOW_TOAST,
        START_ACTIVITY,
        SHOW_RINGTONE_CHOOSER,
        LAUNCH_RINGTONE_PICKER,
        SHOW_AUDIO_ACCESS_RATIONALE,
        REQUEST_AUDIO_RUNTIME,
        OPEN_AUDIO_PERMISSION_SETTINGS,
        SHOW_AUDIO_PERMANENT_DENY_DIALOG
    }

    public final Type type;
    @StringRes
    public final int toastRes;
    public final boolean toastLong;
    @Nullable
    public final Class<? extends Activity> activityClass;
    public final int ringtoneSelectedIndex;
    @Nullable
    public final String existingRingtoneUri;
    @Nullable
    public final String runtimePermission;
    @Nullable
    public final SettingsPermissionHelper.Kind permissionKind;
    @StringRes
    public final int rationaleMessageRes;

    private SettingsHubEffect(Type type,
                              @StringRes int toastRes,
                              boolean toastLong,
                              @Nullable Class<? extends Activity> activityClass,
                              int ringtoneSelectedIndex,
                              @Nullable String existingRingtoneUri,
                              @Nullable String runtimePermission,
                              @Nullable SettingsPermissionHelper.Kind permissionKind,
                              @StringRes int rationaleMessageRes) {
        this.type = type;
        this.toastRes = toastRes;
        this.toastLong = toastLong;
        this.activityClass = activityClass;
        this.ringtoneSelectedIndex = ringtoneSelectedIndex;
        this.existingRingtoneUri = existingRingtoneUri;
        this.runtimePermission = runtimePermission;
        this.permissionKind = permissionKind;
        this.rationaleMessageRes = rationaleMessageRes;
    }

    @NonNull
    public static SettingsHubEffect showToast(@StringRes int resId, boolean longDuration) {
        return new SettingsHubEffect(Type.SHOW_TOAST, resId, longDuration, null, 0, null, null, null, 0);
    }

    @NonNull
    public static SettingsHubEffect startActivity(@NonNull Class<? extends Activity> cls) {
        return new SettingsHubEffect(Type.START_ACTIVITY, 0, false, cls, 0, null, null, null, 0);
    }

    @NonNull
    public static SettingsHubEffect showRingtoneChooser(int selectedIndex) {
        return new SettingsHubEffect(Type.SHOW_RINGTONE_CHOOSER, 0, false, null, selectedIndex, null, null, null, 0);
    }

    @NonNull
    public static SettingsHubEffect launchRingtonePicker(@Nullable String existingUri) {
        return new SettingsHubEffect(Type.LAUNCH_RINGTONE_PICKER, 0, false, null, 0, existingUri, null, null, 0);
    }

    @NonNull
    public static SettingsHubEffect showAudioAccessRationale(@StringRes int messageRes) {
        return new SettingsHubEffect(
                Type.SHOW_AUDIO_ACCESS_RATIONALE, 0, false, null, 0, null, null, null, messageRes);
    }

    @NonNull
    public static SettingsHubEffect requestAudioRuntime(@NonNull String permission,
                                                        @NonNull SettingsPermissionHelper.Kind kind) {
        return new SettingsHubEffect(
                Type.REQUEST_AUDIO_RUNTIME, 0, false, null, 0, null, permission, kind, 0);
    }

    @NonNull
    public static SettingsHubEffect openAudioPermissionSettings(
            @NonNull SettingsPermissionHelper.Kind kind) {
        return new SettingsHubEffect(
                Type.OPEN_AUDIO_PERMISSION_SETTINGS, 0, false, null, 0, null, null, kind, 0);
    }

    @NonNull
    public static SettingsHubEffect showAudioPermanentDenyDialog(
            @NonNull SettingsPermissionHelper.Kind kind) {
        return new SettingsHubEffect(
                Type.SHOW_AUDIO_PERMANENT_DENY_DIALOG, 0, false, null, 0, null, null, kind, 0);
    }
}
