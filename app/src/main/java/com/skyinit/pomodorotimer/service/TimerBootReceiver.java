package com.skyinit.pomodorotimer.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.domain.timer.SessionPhase;
import com.skyinit.pomodorotimer.domain.timer.TimerSessionPolicy;
import com.skyinit.pomodorotimer.util.AppLog;

/**
 * 开机后根据磁盘快照重挂计时 Alarm（不强制拉起 FGS，避免后台启动限制）。
 */
public class TimerBootReceiver extends BroadcastReceiver {

    private static final String TAG = "TimerBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)
                && !"com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(context);
        if (cp == null) {
            return;
        }
        AppLog.d(TAG, "Rescheduling timer alarms after boot, phase=" + cp.phase);
        long nowElapsed = SystemClock.elapsedRealtime();
        long nowWall = System.currentTimeMillis();

        if (cp.phase.isRunning() || cp.running) {
            long remaining = TimerSessionPolicy.resolveRunningRemaining(
                    cp.timerEndElapsedRealtime, cp.timerEndWallClockMs, nowElapsed, nowWall);
            if (remaining <= 0L) {
                // 已到期：直接投递完成，由 Service 结算
                TimerServiceLauncher.deliverAction(context, TimerService.ACTION_SESSION_COMPLETE);
            } else {
                long trigger = nowElapsed + remaining;
                TimerAlarmScheduler.scheduleSessionComplete(context, trigger, cp.generation);
            }
        } else if (cp.phase == SessionPhase.PAUSED || cp.paused) {
            long pauseElapsed = TimerSessionPolicy.resolvePauseElapsed(
                    cp.pauseStartElapsedRealtime, cp.pauseStartWallClockMs, nowElapsed, nowWall);
            long remaining = TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS - pauseElapsed;
            if (remaining <= 0L) {
                TimerServiceLauncher.deliverAction(context, TimerService.ACTION_PAUSE_TIMEOUT);
            } else {
                TimerAlarmScheduler.schedulePauseTimeout(context, nowElapsed + remaining, cp.generation);
            }
        }
    }
}
