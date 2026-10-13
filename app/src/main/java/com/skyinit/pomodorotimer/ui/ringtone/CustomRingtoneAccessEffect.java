package com.skyinit.pomodorotimer.ui.ringtone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;

/**
 * 冷启动自定义铃声降级一次性副作用。
 */
public enum CustomRingtoneAccessEffect {
    SHOW_DEGRADED_DIALOG,
    REQUEST_RUNTIME,
    OPEN_SETTINGS,
    SHOW_TOAST_SETTINGS_FAILED;

    @Nullable
    public static String runtimePermissionOf(@NonNull SettingsPermissionHelper.Kind kind) {
        return SettingsPermissionHelper.getRuntimePermission(kind);
    }
}
