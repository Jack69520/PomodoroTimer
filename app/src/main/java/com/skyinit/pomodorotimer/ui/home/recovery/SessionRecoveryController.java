package com.skyinit.pomodorotimer.ui.home.recovery;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.domain.timer.RecoveryDecision;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;
import com.skyinit.pomodorotimer.ui.home.TimerActivity;
import com.skyinit.pomodorotimer.util.ExactAlarmPermissionHelper;

/**
 * 将 {@link SessionRecoveryViewModel} 绑定到 Activity：强制进计时页 + 现代化结果弹窗。
 */
public final class SessionRecoveryController {

    private SessionRecoveryController() {
    }

    public static void bind(@NonNull FragmentActivity activity) {
        SessionRecoveryViewModel vm = new ViewModelProvider(activity).get(SessionRecoveryViewModel.class);
        if (!vm.markControllerBound()) {
            return;
        }

        vm.getEffect().observe(activity, effect -> {
            if (effect == null) {
                return;
            }
            switch (effect) {
                case FORCE_OPEN_TIMER:
                    openTimer(activity);
                    break;
                case SHOW_RESULT_DIALOG:
                    showResultDialog(activity, vm);
                    break;
                case SHOW_DISCARD_TOAST:
                    Toast.makeText(activity, R.string.recovery_toast_session_invalid, Toast.LENGTH_SHORT).show();
                    break;
                case SHOW_RESTORE_FAILED_TOAST:
                    Toast.makeText(activity, R.string.recovery_toast_restore_failed, Toast.LENGTH_SHORT).show();
                    break;
                case SHOW_EXACT_ALARM_HINT:
                    ExactAlarmPermissionHelper.maybeShowInAppDialog(activity);
                    break;
                default:
                    break;
            }
            vm.consumeEffect();
        });

        vm.dispatch(SessionRecoveryIntent.EVALUATE_STARTUP);
    }

    /** Service 已连接时，若仍在等待则重试评估。 */
    public static void onServiceReady(@NonNull FragmentActivity activity) {
        SessionRecoveryViewModel vm = new ViewModelProvider(activity).get(SessionRecoveryViewModel.class);
        vm.dispatch(SessionRecoveryIntent.SERVICE_READY);
    }

    public static boolean isBlockingShortcuts(@NonNull FragmentActivity activity) {
        SessionRecoveryViewModel vm = new ViewModelProvider(activity).get(SessionRecoveryViewModel.class);
        return vm.isBlockingShortcuts();
    }

    private static void openTimer(FragmentActivity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        Intent intent = new Intent(activity, TimerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
    }

    private static void showResultDialog(FragmentActivity activity, SessionRecoveryViewModel vm) {
        SessionRecoveryUiState state = vm.getUiState().getValue();
        if (state == null || state.phase != SessionRecoveryUiState.Phase.RESULT) {
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        RecoveryDecision.ResultKind kind = state.resultKind;
        ModernPromptDialog.Builder builder = ModernPromptDialog.builder(activity)
                .cancelable(false);

        switch (kind) {
            case BREAK_STARTED:
                builder.icon(R.drawable.ic_timer)
                        .accent(ModernPromptDialog.Accent.SUCCESS)
                        .title(R.string.recovery_result_title_break)
                        .message(R.string.recovery_result_message_break)
                        .primary(R.string.recovery_result_view_break, () ->
                                vm.dispatch(SessionRecoveryIntent.OPEN_TIMER))
                        .secondary(R.string.recovery_result_later, () ->
                                vm.dispatch(SessionRecoveryIntent.DISMISS));
                break;
            case FAILED_TIMEOUT:
                builder.icon(R.drawable.ic_timer)
                        .accent(ModernPromptDialog.Accent.DANGER)
                        .title(R.string.recovery_result_title_failed)
                        .message(R.string.recovery_result_message_failed)
                        .primaryDanger(R.string.recovery_result_dismiss, () ->
                                vm.dispatch(SessionRecoveryIntent.DISMISS));
                break;
            case COMPLETED:
            default:
                builder.icon(R.drawable.ic_timer)
                        .accent(ModernPromptDialog.Accent.SUCCESS)
                        .title(R.string.recovery_result_title_completed)
                        .message(R.string.recovery_result_message_completed)
                        .primary(R.string.recovery_result_dismiss, () ->
                                vm.dispatch(SessionRecoveryIntent.DISMISS))
                        .secondary(R.string.recovery_result_start_new, () ->
                                vm.dispatch(SessionRecoveryIntent.START_NEW_ROUND));
                break;
        }
        builder.show();
    }
}
