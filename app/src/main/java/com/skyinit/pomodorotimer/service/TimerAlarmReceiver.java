package com.skyinit.pomodorotimer.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.skyinit.pomodorotimer.util.AppLog;

/**
 * 接收 AlarmManager 广播，转交 TimerService 处理完成/暂停超时。
 * 进程被杀后 Alarm 仍可唤醒应用恢复逻辑。
 */
public class TimerAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "TimerAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        AppLog.d(TAG, "Alarm received: " + action);
        if (TimerService.ACTION_SESSION_COMPLETE.equals(action)
                || TimerService.ACTION_PAUSE_TIMEOUT.equals(action)) {
            TimerServiceLauncher.deliverAction(context, action);
        }
    }
}
