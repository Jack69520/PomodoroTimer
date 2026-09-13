package com.skyinit.pomodorotimer.ui.settings;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * 设置主页一次性副作用。
 */
public final class SettingsHubEffect {

    public enum Type {
        SHOW_TOAST,
        START_ACTIVITY,
        SHOW_RINGTONE_CHOOSER,
        LAUNCH_RINGTONE_PICKER,
        REQUEST_AUDIO_PERMISSION
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

    private SettingsHubEffect(Type type,
                              @StringRes int toastRes,
                              boolean toastLong,
                              @Nullable Class<? extends Activity> activityClass,
                              int ringtoneSelectedIndex,
                              @Nullable String existingRingtoneUri) {
        this.type = type;
        this.toastRes = toastRes;
        this.toastLong = toastLong;
        this.activityClass = activityClass;
        this.ringtoneSelectedIndex = ringtoneSelectedIndex;
        this.existingRingtoneUri = existingRingtoneUri;
    }

    @NonNull
    public static SettingsHubEffect showToast(@StringRes int resId, boolean longDuration) {
        return new SettingsHubEffect(Type.SHOW_TOAST, resId, longDuration, null, 0, null);
    }

    @NonNull
    public static SettingsHubEffect startActivity(@NonNull Class<? extends Activity> cls) {
        return new SettingsHubEffect(Type.START_ACTIVITY, 0, false, cls, 0, null);
    }

    @NonNull
    public static SettingsHubEffect showRingtoneChooser(int selectedIndex) {
        return new SettingsHubEffect(Type.SHOW_RINGTONE_CHOOSER, 0, false, null, selectedIndex, null);
    }

    @NonNull
    public static SettingsHubEffect launchRingtonePicker(@Nullable String existingUri) {
        return new SettingsHubEffect(Type.LAUNCH_RINGTONE_PICKER, 0, false, null, 0, existingUri);
    }

    @NonNull
    public static SettingsHubEffect requestAudioPermission() {
        return new SettingsHubEffect(Type.REQUEST_AUDIO_PERMISSION, 0, false, null, 0, null);
    }
}
