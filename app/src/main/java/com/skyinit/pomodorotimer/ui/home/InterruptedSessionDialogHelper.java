package com.skyinit.pomodorotimer.ui.home;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 检测到中断的番茄钟时，在应用内弹窗让用户选择继续或结束。
 */
public final class InterruptedSessionDialogHelper {

    private static final String TAG_HANDLED_SESSION = "handled_recovery_session_id";

    private InterruptedSessionDialogHelper() {
    }

    /**
     * 若存在未恢复的中断会话且尚未处理，则弹出对话框。
     *
     * @param activity 宿主 Activity
     */
    public static void showIfNeeded(@NonNull FragmentActivity activity) {
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(activity);
        if (cp == null) {
            return;
        }

        String activeUserId = AccountManager.getInstance(activity).requireActiveUserId();
        if (!cp.belongsToUser(activeUserId)) {
            ActiveSessionStore.clear(activity);
            return;
        }

        long handledId = activity.getSharedPreferences("PomodoroPrefs", 0)
                .getLong(TAG_HANDLED_SESSION, -1L);
        if (handledId == cp.sessionStartTime) {
            return;
        }

        long remaining = ActiveSessionStore.computeRemainingMillis(cp);
        long elapsed = ActiveSessionStore.computeElapsedMillis(cp);
        boolean isStudy = cp.sessionType == 0;
        boolean saveEligible = ActiveSessionStore.isSaveEligible(cp);
        boolean alreadyFinished = remaining <= 0L;

        String message = buildMessage(activity, cp, remaining, elapsed, alreadyFinished);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(alreadyFinished
                        ? R.string.recovery_dialog_title_finished
                        : R.string.recovery_dialog_title)
                .setMessage(message)
                .setCancelable(false);

        if (alreadyFinished) {
            // 计时已到点：仅提供结束选项（可保存或丢弃）
            if (saveEligible) {
                builder.setPositiveButton(R.string.recovery_end_save, (d, w) -> {
                    markHandled(activity, cp.sessionStartTime);
                    endSession(activity, true);
                });
                builder.setNegativeButton(R.string.recovery_end_discard, (d, w) -> {
                    markHandled(activity, cp.sessionStartTime);
                    endSession(activity, false);
                });
            } else {
                builder.setPositiveButton(R.string.recovery_end, (d, w) -> {
                    markHandled(activity, cp.sessionStartTime);
                    endSession(activity, false);
                });
            }
        } else if (saveEligible) {
            // 进行中且可保存：一站式三选项
            builder.setPositiveButton(R.string.recovery_continue, (d, w) -> {
                markHandled(activity, cp.sessionStartTime);
                continueSession(activity);
            });
            builder.setNeutralButton(R.string.recovery_end_save, (d, w) -> {
                markHandled(activity, cp.sessionStartTime);
                endSession(activity, true);
            });
            builder.setNegativeButton(R.string.recovery_end_discard, (d, w) -> {
                markHandled(activity, cp.sessionStartTime);
                endSession(activity, false);
            });
        } else {
            builder.setPositiveButton(R.string.recovery_continue, (d, w) -> {
                markHandled(activity, cp.sessionStartTime);
                continueSession(activity);
            });
            builder.setNegativeButton(R.string.recovery_end, (d, w) -> {
                markHandled(activity, cp.sessionStartTime);
                endSession(activity, false);
            });
        }

        builder.show();
    }

    private static String buildMessage(FragmentActivity activity,
                                       ActiveSessionStore.Checkpoint cp,
                                       long remainingMs,
                                       long elapsedMs,
                                       boolean alreadyFinished) {
        String typeLabel = cp.sessionType == 0
                ? activity.getString(R.string.recovery_session_study)
                : activity.getString(R.string.recovery_session_break);
        String timeLabel = alreadyFinished
                ? activity.getString(R.string.recovery_time_finished)
                : activity.getString(R.string.recovery_time_remaining, formatDuration(remainingMs));
        String elapsedLabel = activity.getString(R.string.recovery_time_elapsed, formatDuration(elapsedMs));

        StringBuilder sb = new StringBuilder();
        sb.append(typeLabel);
        if (cp.taskTitle != null && !cp.taskTitle.isEmpty()) {
            sb.append("\n").append(activity.getString(R.string.recovery_task, cp.taskTitle));
        }
        sb.append("\n").append(timeLabel);
        sb.append("\n").append(elapsedLabel);

        if (cp.sessionType == 0) {
            if (ActiveSessionStore.isSaveEligible(cp)) {
                sb.append("\n\n").append(activity.getString(R.string.recovery_save_hint));
            } else {
                sb.append("\n\n").append(activity.getString(R.string.recovery_discard_hint));
            }
        }
        return sb.toString();
    }

    private static void continueSession(FragmentActivity activity) {
        TimerServiceLauncher.deliverAction(activity, TimerService.ACTION_RESTORE_SESSION);
        activity.startActivity(new Intent(activity, TimerActivity.class));
        Toast.makeText(activity, R.string.recovery_toast_continued, Toast.LENGTH_SHORT).show();
    }

    private static void endSession(FragmentActivity activity, boolean saveRecord) {
        android.content.Intent intent = new android.content.Intent(activity, TimerService.class);
        intent.setAction(TimerService.ACTION_END_INTERRUPTED);
        intent.putExtra(TimerService.EXTRA_SAVE_RECORD, saveRecord);
        TimerServiceLauncher.deliverAction(activity, intent);

        int toastRes = saveRecord
                ? R.string.recovery_toast_saved
                : R.string.recovery_toast_discarded;
        Toast.makeText(activity, toastRes, Toast.LENGTH_SHORT).show();
    }

    private static void markHandled(FragmentActivity activity, long sessionStartTime) {
        activity.getSharedPreferences("PomodoroPrefs", 0)
                .edit()
                .putLong(TAG_HANDLED_SESSION, sessionStartTime)
                .apply();
    }

    private static String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
