package com.skyinit.pomodorotimer.ui.settings;

import android.app.Application;
import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 系统权限页 ViewModel：Intent → {@link SystemPermissionsUiState} + {@link SystemPermissionsEffect}。
 * 权限申请用 inFlight 防呆。
 */
public class SystemPermissionsViewModel extends ViewModel {

    private final Application application;

    private final MutableLiveData<SystemPermissionsUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<SystemPermissionsEffect> effects = new SingleLiveEvent<>();

    private final AtomicBoolean permissionRequestInFlight = new AtomicBoolean(false);
    @Nullable
    private volatile SettingsPermissionHelper.Kind pendingRuntimeKind;

    public SystemPermissionsViewModel(@NonNull Application application) {
        this.application = application;
        publishSnapshot(buildSnapshot(), false);
        dispatch(SystemPermissionsIntent.refresh());
    }

    @NonNull
    public LiveData<SystemPermissionsUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<SystemPermissionsEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull SystemPermissionsIntent intent) {
        switch (intent.type) {
            case REFRESH:
                refresh();
                break;
            case REQUEST_PERMISSION:
                onRequestPermission(intent.permissionKind);
                break;
            default:
                break;
        }
    }

    @Nullable
    public SettingsPermissionHelper.Kind getPendingRuntimePermissionKind() {
        return pendingRuntimeKind;
    }

    /**
     * 运行时权限回调：Activity 在生命周期安全时调用。
     *
     * @param openSettingsFallback true 表示系统不再弹出授权框（永久拒绝等），需跳转设置页兜底
     */
    @MainThread
    public void onRuntimePermissionResult(boolean granted, boolean openSettingsFallback) {
        SettingsPermissionHelper.Kind kind = pendingRuntimeKind;
        pendingRuntimeKind = null;
        permissionRequestInFlight.set(false);
        // 仅永久拒绝 / 无法再弹系统框时跳转设置；普通「拒绝」保留下次再点可再次唤起系统框
        if (!granted && openSettingsFallback && kind != null) {
            effects.setValue(SystemPermissionsEffect.openPermissionSettings(kind));
        }
        refresh();
    }

    /** 跳转系统设置失败时释放 inFlight，避免按钮永久禁用。 */
    @MainThread
    public void onPermissionNavigateFailed() {
        pendingRuntimeKind = null;
        permissionRequestInFlight.set(false);
        refresh();
    }

    /** UI 判定应直达系统设置时，清除 pending 运行时种类，避免 onResume 卡死 inFlight。 */
    @MainThread
    public void onPermissionDivertedToSettings() {
        pendingRuntimeKind = null;
    }

    /** 从系统设置页返回或 onResume 时释放卡死的 inFlight。 */
    @MainThread
    public void onHostResumed() {
        if (pendingRuntimeKind == null) {
            permissionRequestInFlight.set(false);
        }
        refresh();
    }

    private void refresh() {
        publishSnapshot(buildSnapshot(), permissionRequestInFlight.get());
    }

    private void onRequestPermission(@Nullable SettingsPermissionHelper.Kind kind) {
        if (kind == null) {
            return;
        }
        Context context = application;
        if (!SettingsPermissionHelper.isApplicable(kind)
                || SettingsPermissionHelper.isGranted(context, kind)) {
            refresh();
            return;
        }
        if (!permissionRequestInFlight.compareAndSet(false, true)) {
            return;
        }
        publishSnapshot(buildSnapshot(), true);

        // shouldShowRequestPermissionRationale 需要 Activity，直接进设置的判定交给 UI 层。
        if (SettingsPermissionHelper.requiresSettingsIntent(kind)) {
            effects.setValue(SystemPermissionsEffect.openPermissionSettings(kind));
            return;
        }

        String permission = SettingsPermissionHelper.getRuntimePermission(kind);
        if (permission == null) {
            permissionRequestInFlight.set(false);
            refresh();
            return;
        }
        // 勿在此处 markRequested：须等 UI 判定不会直达设置、真正 launch 系统授权框后再标记，
        // 否则首次点击会被误判为「已请求过且无 rationale」而跳过系统弹窗。
        pendingRuntimeKind = kind;
        effects.setValue(SystemPermissionsEffect.requestRuntimePermission(kind, permission));
    }

    @NonNull
    private SystemPermissionsUiState buildSnapshot() {
        Context context = application;
        Map<SettingsPermissionHelper.Kind, SystemPermissionsUiState.PermissionRow> map =
                new EnumMap<>(SettingsPermissionHelper.Kind.class);
        for (SettingsPermissionHelper.Kind kind : SettingsPermissionHelper.Kind.values()) {
            boolean visible = SettingsPermissionHelper.isApplicable(kind);
            boolean granted = visible && SettingsPermissionHelper.isGranted(context, kind);
            map.put(kind, new SystemPermissionsUiState.PermissionRow(visible, granted, visible && !granted));
        }
        return new SystemPermissionsUiState(permissionRequestInFlight.get(), map);
    }

    private void publishSnapshot(@NonNull SystemPermissionsUiState state, boolean inFlight) {
        uiState.setValue(inFlight == state.permissionRequestInFlight
                ? state
                : state.withPermissionInFlight(inFlight));
    }
}
