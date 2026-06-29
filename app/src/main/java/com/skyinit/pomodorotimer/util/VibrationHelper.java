package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * 通知权限不可用时的触觉提醒兜底。
 */
public final class VibrationHelper {

    private static final long[] ALERT_PATTERN = {0L, 500L, 250L, 500L};

    private VibrationHelper() {
    }

    public static void vibrateAlert(Context context) {
        if (context == null) {
            return;
        }
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(ALERT_PATTERN, -1));
            } else {
                vibrator.vibrate(ALERT_PATTERN, -1);
            }
        } catch (Exception ignored) {
        }
    }
}
