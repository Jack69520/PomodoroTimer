package com.skyinit.pomodorotimer.ui.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;

/**
 * 系统权限：集中展示并申请本应用所需各项权限。
 */
public class SystemPermissionsActivity extends SubpageActivity {

    private SystemPermissionsViewModel viewModel;
    private ActivityResultLauncher<String> runtimePermissionLauncher;

    private View permissionRowNotification;
    private View permissionRowMediaFiles;
    private View permissionRowMusicAudio;
    private View permissionRowPhotosVideos;
    private View permissionRowCamera;
    private View permissionRowExactAlarm;
    private View permissionRowBatteryOptimization;
    private View permissionRowUsageStats;
    private View permissionRowOverlay;
    private TextView permissionStatusNotification;
    private TextView permissionStatusMediaFiles;
    private TextView permissionStatusMusicAudio;
    private TextView permissionStatusPhotosVideos;
    private TextView permissionStatusCamera;
    private TextView permissionStatusExactAlarm;
    private TextView permissionStatusBatteryOptimization;
    private TextView permissionStatusUsageStats;
    private TextView permissionStatusOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_system_permissions, R.string.settings_system_permissions);

        viewModel = new ViewModelProvider(
                this,
                ((App) getApplication()).getContainer().getViewModelFactory()
        ).get(SystemPermissionsViewModel.class);

        bindViews();
        registerLaunchers();
        bindClicks();
        observeVm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.onHostResumed();
    }

    private void bindViews() {
        permissionRowNotification = findViewById(R.id.permission_row_notification);
        permissionRowMediaFiles = findViewById(R.id.permission_row_media_files);
        permissionRowMusicAudio = findViewById(R.id.permission_row_music_audio);
        permissionRowPhotosVideos = findViewById(R.id.permission_row_photos_videos);
        permissionRowCamera = findViewById(R.id.permission_row_camera);
        permissionRowExactAlarm = findViewById(R.id.permission_row_exact_alarm);
        permissionRowBatteryOptimization = findViewById(R.id.permission_row_battery_optimization);
        permissionRowUsageStats = findViewById(R.id.permission_row_usage_stats);
        permissionRowOverlay = findViewById(R.id.permission_row_overlay);
        permissionStatusNotification = findViewById(R.id.permission_status_notification);
        permissionStatusMediaFiles = findViewById(R.id.permission_status_media_files);
        permissionStatusMusicAudio = findViewById(R.id.permission_status_music_audio);
        permissionStatusPhotosVideos = findViewById(R.id.permission_status_photos_videos);
        permissionStatusCamera = findViewById(R.id.permission_status_camera);
        permissionStatusExactAlarm = findViewById(R.id.permission_status_exact_alarm);
        permissionStatusBatteryOptimization = findViewById(R.id.permission_status_battery_optimization);
        permissionStatusUsageStats = findViewById(R.id.permission_status_usage_stats);
        permissionStatusOverlay = findViewById(R.id.permission_status_overlay);
    }

    private void registerLaunchers() {
        runtimePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    SettingsPermissionHelper.Kind kind = viewModel.getPendingRuntimePermissionKind();
                    boolean openSettingsFallback = !granted
                            && kind != null
                            && SettingsPermissionHelper.shouldOpenSettingsDirectly(this, kind);
                    viewModel.onRuntimePermissionResult(granted, openSettingsFallback);
                });
    }

    private void bindClicks() {
        bindPermissionClick(permissionRowNotification, SettingsPermissionHelper.Kind.NOTIFICATION);
        bindPermissionClick(permissionRowMediaFiles, SettingsPermissionHelper.Kind.MEDIA_FILES);
        bindPermissionClick(permissionRowMusicAudio, SettingsPermissionHelper.Kind.MUSIC_AUDIO);
        bindPermissionClick(permissionRowPhotosVideos, SettingsPermissionHelper.Kind.PHOTOS_VIDEOS);
        bindPermissionClick(permissionRowCamera, SettingsPermissionHelper.Kind.CAMERA);
        bindPermissionClick(permissionRowExactAlarm, SettingsPermissionHelper.Kind.EXACT_ALARM);
        bindPermissionClick(permissionRowBatteryOptimization, SettingsPermissionHelper.Kind.BATTERY_OPTIMIZATION);
        bindPermissionClick(permissionRowUsageStats, SettingsPermissionHelper.Kind.USAGE_STATS_BLOCKING);
        bindPermissionClick(permissionRowOverlay, SettingsPermissionHelper.Kind.OVERLAY_BLOCKING);
    }

    private void bindPermissionClick(@Nullable View row, @NonNull SettingsPermissionHelper.Kind kind) {
        if (row == null) {
            return;
        }
        row.setOnClickListener(v -> viewModel.dispatch(SystemPermissionsIntent.requestPermission(kind)));
    }

    private void observeVm() {
        viewModel.getUiState().observe(this, this::render);
        viewModel.getEffects().observe(this, this::handleEffect);
    }

    private void render(@Nullable SystemPermissionsUiState state) {
        if (state == null || isFinishing() || isDestroyed()) {
            return;
        }
        renderPermission(permissionRowNotification, permissionStatusNotification,
                state.permission(SettingsPermissionHelper.Kind.NOTIFICATION));
        renderPermission(permissionRowMediaFiles, permissionStatusMediaFiles,
                state.permission(SettingsPermissionHelper.Kind.MEDIA_FILES));
        renderPermission(permissionRowMusicAudio, permissionStatusMusicAudio,
                state.permission(SettingsPermissionHelper.Kind.MUSIC_AUDIO));
        renderPermission(permissionRowPhotosVideos, permissionStatusPhotosVideos,
                state.permission(SettingsPermissionHelper.Kind.PHOTOS_VIDEOS));
        renderPermission(permissionRowCamera, permissionStatusCamera,
                state.permission(SettingsPermissionHelper.Kind.CAMERA));
        renderPermission(permissionRowExactAlarm, permissionStatusExactAlarm,
                state.permission(SettingsPermissionHelper.Kind.EXACT_ALARM));
        renderPermission(permissionRowBatteryOptimization, permissionStatusBatteryOptimization,
                state.permission(SettingsPermissionHelper.Kind.BATTERY_OPTIMIZATION));
        renderPermission(permissionRowUsageStats, permissionStatusUsageStats,
                state.permission(SettingsPermissionHelper.Kind.USAGE_STATS_BLOCKING));
        renderPermission(permissionRowOverlay, permissionStatusOverlay,
                state.permission(SettingsPermissionHelper.Kind.OVERLAY_BLOCKING));
    }

    private void renderPermission(@Nullable View row,
                                  @Nullable TextView status,
                                  @NonNull SystemPermissionsUiState.PermissionRow data) {
        if (row == null || status == null) {
            return;
        }
        row.setVisibility(data.visible ? View.VISIBLE : View.GONE);
        if (!data.visible) {
            return;
        }
        if (data.granted) {
            status.setText(R.string.settings_permission_status_granted);
            status.setTextColor(ContextCompat.getColor(this, R.color.comp_alert_break));
            row.setClickable(false);
            row.setEnabled(false);
        } else {
            status.setText(R.string.settings_permission_status_denied);
            status.setTextColor(ContextCompat.getColor(this, R.color.semantic_error));
            row.setClickable(true);
            row.setEnabled(true);
        }
    }

    private void handleEffect(@Nullable SystemPermissionsEffect effect) {
        if (effect == null || isFinishing() || isDestroyed()) {
            return;
        }
        switch (effect.type) {
            case SHOW_TOAST:
                Toast.makeText(this, effect.toastRes,
                        effect.toastLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
                break;
            case REQUEST_RUNTIME_PERMISSION:
                // 优先系统授权弹窗；仅永久拒绝等无法再弹框时才 Intent 到应用权限设置
                if (effect.permissionKind != null
                        && SettingsPermissionHelper.shouldOpenSettingsDirectly(
                        this, effect.permissionKind)) {
                    viewModel.onPermissionDivertedToSettings();
                    if (!SettingsPermissionHelper.openPermissionSettings(this, effect.permissionKind)) {
                        Toast.makeText(this, R.string.settings_permission_open_settings_failed,
                                Toast.LENGTH_SHORT).show();
                        viewModel.onPermissionNavigateFailed();
                    }
                    break;
                }
                if (effect.runtimePermission != null) {
                    SettingsPermissionHelper.markRequested(this, effect.runtimePermission);
                    runtimePermissionLauncher.launch(effect.runtimePermission);
                } else {
                    viewModel.onPermissionNavigateFailed();
                }
                break;
            case OPEN_PERMISSION_SETTINGS:
                if (effect.permissionKind != null) {
                    if (!SettingsPermissionHelper.openPermissionSettings(this, effect.permissionKind)) {
                        Toast.makeText(this, R.string.settings_permission_open_settings_failed,
                                Toast.LENGTH_SHORT).show();
                        viewModel.onPermissionNavigateFailed();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
