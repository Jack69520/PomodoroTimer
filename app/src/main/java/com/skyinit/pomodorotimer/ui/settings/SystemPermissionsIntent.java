package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;

import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;

/**
 * 系统权限页用户意图。
 */
public final class SystemPermissionsIntent {

    public enum Type {
        REFRESH,
        REQUEST_PERMISSION
    }

    public final Type type;
    public final SettingsPermissionHelper.Kind permissionKind;

    private SystemPermissionsIntent(Type type, SettingsPermissionHelper.Kind permissionKind) {
        this.type = type;
        this.permissionKind = permissionKind;
    }

    @NonNull
    public static SystemPermissionsIntent refresh() {
        return new SystemPermissionsIntent(Type.REFRESH, null);
    }

    @NonNull
    public static SystemPermissionsIntent requestPermission(@NonNull SettingsPermissionHelper.Kind kind) {
        return new SystemPermissionsIntent(Type.REQUEST_PERMISSION, kind);
    }
}
