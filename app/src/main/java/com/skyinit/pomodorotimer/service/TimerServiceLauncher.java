package com.skyinit.pomodorotimer.service;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

/**
 * 统一通过 startForegroundService 启动 TimerService，满足 API 26+ 前台服务规范。
 */
public final class TimerServiceLauncher {

    private TimerServiceLauncher() {
    }

    public static void ensureRunning(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, TimerService.class));
    }

    public static void deliverAction(Context context, String action) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(action);
        ContextCompat.startForegroundService(context, intent);
    }

    /** 携带 Intent extras 启动（如暂停原因）。 */
    public static void deliverAction(Context context, Intent intent) {
        ContextCompat.startForegroundService(context, intent);
    }
}
