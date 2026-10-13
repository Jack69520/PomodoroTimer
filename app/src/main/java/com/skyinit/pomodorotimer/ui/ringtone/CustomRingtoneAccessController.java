package com.skyinit.pomodorotimer.ui.ringtone;

import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;
import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;

/**
 * 将 {@link CustomRingtoneAccessViewModel} 绑定到 MainActivity：
 * 冷启动 degraded 提示 + 快速授权 / 去设置。
 */
public final class CustomRingtoneAccessController {

    private CustomRingtoneAccessController() {
    }

    public static void bind(@NonNull FragmentActivity activity,
                            @NonNull ActivityResultLauncher<String> audioPermissionLauncher) {
        CustomRingtoneAccessViewModel vm = new ViewModelProvider(activity)
                .get(CustomRingtoneAccessViewModel.class);
        if (!vm.markControllerBound()) {
            return;
        }

        vm.getEffects().observe(activity, effect -> {
            if (effect == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            switch (effect) {
                case SHOW_DEGRADED_DIALOG:
                    showDegradedDialog(activity, vm);
                    break;
                case REQUEST_RUNTIME:
                    launchRuntime(activity, vm, audioPermissionLauncher);
                    break;
                case OPEN_SETTINGS:
                    if (!SettingsPermissionHelper.openPermissionSettings(activity, vm.audioKind())) {
                        vm.dispatch(CustomRingtoneAccessIntent.SETTINGS_NAVIGATE_FAILED);
                    }
                    break;
                case SHOW_TOAST_SETTINGS_FAILED:
                    Toast.makeText(activity, R.string.settings_permission_open_settings_failed,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            vm.consumeEffect();
        });

        // 与通知权限系统框错峰：延后到下一帧再评估
        activity.getWindow().getDecorView().post(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            vm.dispatch(CustomRingtoneAccessIntent.EVALUATE_COLD_START);
        });
    }

    public static void onHostResumed(@NonNull FragmentActivity activity) {
        CustomRingtoneAccessViewModel vm = new ViewModelProvider(activity)
                .get(CustomRingtoneAccessViewModel.class);
        vm.dispatch(CustomRingtoneAccessIntent.HOST_RESUMED);
    }

    public static void onRuntimePermissionResult(@NonNull FragmentActivity activity, boolean granted) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        CustomRingtoneAccessViewModel vm = new ViewModelProvider(activity)
                .get(CustomRingtoneAccessViewModel.class);
        if (granted) {
            vm.dispatch(CustomRingtoneAccessIntent.RUNTIME_RESULT_GRANTED);
            return;
        }
        SettingsPermissionHelper.Kind kind = vm.audioKind();
        if (SettingsPermissionHelper.shouldOpenSettingsDirectly(activity, kind)) {
            vm.dispatch(CustomRingtoneAccessIntent.RUNTIME_RESULT_PERMANENT_DENY);
        } else {
            vm.dispatch(CustomRingtoneAccessIntent.RUNTIME_RESULT_DENIED);
        }
    }

    private static void showDegradedDialog(@NonNull FragmentActivity activity,
                                           @NonNull CustomRingtoneAccessViewModel vm) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        AlertDialog dialog = ModernPromptDialog.builder(activity)
                .icon(R.drawable.ic_ringtone)
                .accent(ModernPromptDialog.Accent.BRAND)
                .title(R.string.ringtone_degraded_dialog_title)
                .message(R.string.ringtone_degraded_dialog_message)
                .primary(R.string.ringtone_btn_reauth,
                        () -> vm.dispatch(CustomRingtoneAccessIntent.PRIMARY_GRANT))
                .secondary(R.string.ringtone_btn_open_settings,
                        () -> vm.dispatch(CustomRingtoneAccessIntent.OPEN_SETTINGS))
                .tertiary(R.string.ringtone_btn_later,
                        () -> vm.dispatch(CustomRingtoneAccessIntent.DISMISS))
                .cancelable(true)
                .show();
        dialog.setOnCancelListener(d -> vm.dispatch(CustomRingtoneAccessIntent.DISMISS));
    }

    private static void launchRuntime(@NonNull FragmentActivity activity,
                                      @NonNull CustomRingtoneAccessViewModel vm,
                                      @NonNull ActivityResultLauncher<String> launcher) {
        String permission = vm.runtimePermission();
        SettingsPermissionHelper.Kind kind = vm.audioKind();
        if (permission == null) {
            vm.onDivertedToSettings();
            if (!SettingsPermissionHelper.openPermissionSettings(activity, kind)) {
                vm.dispatch(CustomRingtoneAccessIntent.SETTINGS_NAVIGATE_FAILED);
            }
            return;
        }
        if (SettingsPermissionHelper.shouldOpenSettingsDirectly(activity, kind)) {
            vm.onDivertedToSettings();
            if (!SettingsPermissionHelper.openPermissionSettings(activity, kind)) {
                vm.dispatch(CustomRingtoneAccessIntent.SETTINGS_NAVIGATE_FAILED);
            }
            return;
        }
        SettingsPermissionHelper.markRequested(activity, permission);
        launcher.launch(permission);
    }
}
