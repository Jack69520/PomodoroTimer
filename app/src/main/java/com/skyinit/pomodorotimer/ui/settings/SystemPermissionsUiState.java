package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;

import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 系统权限页不可变 UI 快照。
 */
public final class SystemPermissionsUiState {

    public static final class PermissionRow {
        public final boolean visible;
        public final boolean granted;
        public final boolean actionable;

        public PermissionRow(boolean visible, boolean granted, boolean actionable) {
            this.visible = visible;
            this.granted = granted;
            this.actionable = actionable;
        }
    }

    public final boolean permissionRequestInFlight;
    @NonNull
    public final Map<SettingsPermissionHelper.Kind, PermissionRow> permissions;

    public SystemPermissionsUiState(boolean permissionRequestInFlight,
                                    @NonNull Map<SettingsPermissionHelper.Kind, PermissionRow> permissions) {
        this.permissionRequestInFlight = permissionRequestInFlight;
        this.permissions = Collections.unmodifiableMap(new EnumMap<>(permissions));
    }

    @NonNull
    public PermissionRow permission(@NonNull SettingsPermissionHelper.Kind kind) {
        PermissionRow row = permissions.get(kind);
        if (row == null) {
            return new PermissionRow(false, true, false);
        }
        return row;
    }

    @NonNull
    public SystemPermissionsUiState withPermissionInFlight(boolean inFlight) {
        return new SystemPermissionsUiState(inFlight, permissions);
    }
}
