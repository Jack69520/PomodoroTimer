package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;

/**
 * 系统权限页一次性副作用。
 */
public final class SystemPermissionsEffect {

    public enum Type {
        SHOW_TOAST,
        REQUEST_RUNTIME_PERMISSION,
        OPEN_PERMISSION_SETTINGS
    }

    public final Type type;
    @StringRes
    public final int toastRes;
    public final boolean toastLong;
    @Nullable
    public final SettingsPermissionHelper.Kind permissionKind;
    @Nullable
    public final String runtimePermission;

    private SystemPermissionsEffect(Type type,
                                    @StringRes int toastRes,
                                    boolean toastLong,
                                    @Nullable SettingsPermissionHelper.Kind permissionKind,
                                    @Nullable String runtimePermission) {
        this.type = type;
        this.toastRes = toastRes;
        this.toastLong = toastLong;
        this.permissionKind = permissionKind;
        this.runtimePermission = runtimePermission;
    }

    @NonNull
    public static SystemPermissionsEffect showToast(@StringRes int resId, boolean longDuration) {
        return new SystemPermissionsEffect(Type.SHOW_TOAST, resId, longDuration, null, null);
    }

    @NonNull
    public static SystemPermissionsEffect requestRuntimePermission(
            @NonNull SettingsPermissionHelper.Kind kind,
            @NonNull String permission) {
        return new SystemPermissionsEffect(Type.REQUEST_RUNTIME_PERMISSION, 0, false, kind, permission);
    }

    @NonNull
    public static SystemPermissionsEffect openPermissionSettings(
            @NonNull SettingsPermissionHelper.Kind kind) {
        return new SystemPermissionsEffect(Type.OPEN_PERMISSION_SETTINGS, 0, false, kind, null);
    }
}
