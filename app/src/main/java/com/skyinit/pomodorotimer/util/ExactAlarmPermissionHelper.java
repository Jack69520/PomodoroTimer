package com.skyinit.pomodorotimer.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skyinit.pomodorotimer.R;

/**
 * 精确闹钟权限：应用内弹窗说明，仅在用户主动点击时才跳转系统设置。
 * 未授权时 {@link com.skyinit.pomodorotimer.service.TimerAlarmScheduler} 会自动降级为非精确 Alarm。
 */
public final class ExactAlarmPermissionHelper {

    private static final String PREFS_NAME = "PomodoroPrefs";
    private static final String KEY_DIALOG_SHOWN = "exact_alarm_in_app_dialog_shown";

    private ExactAlarmPermissionHelper() {
    }

    /** 当前环境是否允许调度精确闹钟。API 31 以下恒为 true。 */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    /**
     * 首次需要时在应用内弹窗说明，不自动跳转系统设置页。
     * 用户选择「暂不开启」后静默使用非精确 Alarm，不再打扰。
     */
    public static void maybeShowInAppDialog(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        if (canScheduleExactAlarms(activity)) {
            return;
        }
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_DIALOG_SHOWN, false)) {
            return;
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.exact_alarm_dialog_title)
                .setMessage(R.string.exact_alarm_dialog_message)
                .setNegativeButton(R.string.exact_alarm_dialog_later, (dialog, which) ->
                        prefs.edit().putBoolean(KEY_DIALOG_SHOWN, true).apply())
                .setPositiveButton(R.string.exact_alarm_dialog_enable, (dialog, which) -> {
                    prefs.edit().putBoolean(KEY_DIALOG_SHOWN, true).apply();
                    openExactAlarmSettings(activity);
                })
                .setCancelable(true)
                .show();
    }

    /** 用户主动点击「去开启」时跳转系统「闹钟与提醒」授权页。 */
    public static void openExactAlarmSettings(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        android.content.Intent intent = new android.content.Intent(
                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }
}
