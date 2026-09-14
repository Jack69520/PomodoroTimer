package com.skyinit.pomodorotimer.util;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.home.LockScreenTimerActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 锁屏全屏计时强拉回调度。
 * <p>
 * 混合亮屏：不计时主动灭屏后不强行唤醒；仅在用户再次亮屏且处于锁屏时展示。
 * present 入口以 {@link #onScreenOn()} 为主，灭屏导致的 Activity stop 不得 tryPresent。
 */
public final class LockScreenPresenter {

    public static final String EXTRA_LOCK_PRESENTATION = "extra_lock_presentation";
    public static final String CHANNEL_ID_LOCK_PRESENT = "TimerLockPresentChannel";
    public static final int NOTIFICATION_ID_LOCK_PRESENT = 7;

    private static final String TAG = "LockScreenPresenter";
    private static final long PRESENT_DEBOUNCE_MS = 1500L;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final AtomicBoolean suppressUntilUnlocked = new AtomicBoolean(false);
    private final AtomicBoolean presentInFlight = new AtomicBoolean(false);
    private final AtomicBoolean normalTimerUiResumed = new AtomicBoolean(false);
    private final AtomicBoolean lockScreenUiResumed = new AtomicBoolean(false);
    /** 最近一次收到 SCREEN_OFF 且尚未 SCREEN_ON；用于挡住灭屏引发的 stop→present。 */
    private final AtomicBoolean screenOff = new AtomicBoolean(false);

    private volatile boolean preferenceEnabled;
    private volatile boolean sessionEligible;
    private volatile long lastPresentAtMs;
    private volatile boolean wasEligible;

    public LockScreenPresenter(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        ensureChannel();
    }

    @MainThread
    public void updateSession(boolean preferenceEnabled, boolean isRunning, boolean isPaused) {
        this.preferenceEnabled = preferenceEnabled;
        this.sessionEligible = LockScreenTimerGate.isSessionEligible(isRunning, isPaused);
        boolean eligibleNow = this.preferenceEnabled && this.sessionEligible;
        if (!eligibleNow) {
            cancelPresentNotification();
            suppressUntilUnlocked.set(false);
            wasEligible = false;
            return;
        }
        // 会话刚变为可展示：仅屏亮着时尝试；熄屏中交给后续 SCREEN_ON
        if (!wasEligible && !isAnyTimerUiResumed()) {
            tryPresent("session_became_eligible");
        }
        wasEligible = true;
    }

    @MainThread
    public void onNormalTimerUiResumed() {
        normalTimerUiResumed.set(true);
        presentInFlight.set(false);
        cancelPresentNotification();
    }

    @MainThread
    public void onNormalTimerUiStopped(boolean dueToConfigChange) {
        normalTimerUiResumed.set(false);
        if (dueToConfigChange) {
            return;
        }
        // 用户离开普通计时页：仅在屏已亮且锁屏时强拉回；灭屏中不拉起（避免唤醒）
        tryPresent("normal_ui_stopped");
    }

    @MainThread
    public void onLockScreenUiResumed() {
        lockScreenUiResumed.set(true);
        presentInFlight.set(false);
        cancelPresentNotification();
    }

    @MainThread
    public void onLockScreenUiStopped(boolean dueToConfigChange) {
        lockScreenUiResumed.set(false);
        // 灭屏 / 长按解锁 / 其它原因导致 stop：一律不在此处 present。
        // 重新展示只走 onScreenOn（混合亮屏硬约束）。
    }

    @MainThread
    public void onUserRequestedUnlock() {
        suppressUntilUnlocked.set(true);
        cancelPresentNotification();
        presentInFlight.set(false);
    }

    @MainThread
    public void onUserUnlocked() {
        suppressUntilUnlocked.set(false);
    }

    /**
     * 用户主动亮屏：若处于锁屏则展示锁屏计时页（唯一主入口）。
     */
    @MainThread
    public void onScreenOn() {
        screenOff.set(false);
        if (isKeyguardLocked()) {
            // USER_UNLOCKED/PRESENT 在部分机型可能收不到，亮屏+锁屏时清除抑制
            suppressUntilUnlocked.set(false);
        }
        tryPresent("screen_on");
    }

    /**
     * 用户主动灭屏：禁止任何 present / 唤醒；清抑制与防抖，供下次亮屏使用。
     */
    @MainThread
    public void onScreenOff() {
        screenOff.set(true);
        presentInFlight.set(false);
        cancelPresentNotification();
        // 不在此处 tryPresent（混合策略：灭屏不强唤醒）
        suppressUntilUnlocked.set(false);
        lastPresentAtMs = 0L;
    }

    @MainThread
    public void teardown() {
        preferenceEnabled = false;
        sessionEligible = false;
        wasEligible = false;
        suppressUntilUnlocked.set(false);
        presentInFlight.set(false);
        normalTimerUiResumed.set(false);
        lockScreenUiResumed.set(false);
        screenOff.set(false);
        cancelPresentNotification();
        mainHandler.removeCallbacksAndMessages(null);
    }

    public boolean isPreferenceEnabled() {
        return preferenceEnabled;
    }

    public boolean isSessionEligible() {
        return sessionEligible;
    }

    public boolean isTimerUiResumed() {
        return isAnyTimerUiResumed();
    }

    public boolean isSuppressUntilUnlocked() {
        return suppressUntilUnlocked.get();
    }

    private boolean isAnyTimerUiResumed() {
        return normalTimerUiResumed.get() || lockScreenUiResumed.get();
    }

    @MainThread
    private void tryPresent(@NonNull String reason) {
        if (!shouldPresent()) {
            return;
        }
        if (isAnyTimerUiResumed()) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastPresentAtMs < PRESENT_DEBOUNCE_MS) {
            return;
        }
        if (!presentInFlight.compareAndSet(false, true)) {
            return;
        }
        lastPresentAtMs = now;
        AppLog.d(TAG, "tryPresent reason=" + reason);

        boolean started = tryStartActivityDirect();
        if (started) {
            mainHandler.postDelayed(() -> presentInFlight.set(false), 2500L);
            return;
        }

        // FSI 仅在屏已亮的路径作为降级；熄屏中 shouldPresent 已为 false，不会走到这里唤醒屏幕
        if (LockScreenTimerGate.canUseFullScreenIntent(appContext)
                && LockScreenTimerGate.hasNotificationPermission(appContext)) {
            fireFullScreenIntentNotification();
            mainHandler.postDelayed(() -> presentInFlight.set(false), 2500L);
        } else {
            AppLog.d(TAG, "Degrade to notification-only (no FSI or notification permission)");
            presentInFlight.set(false);
        }
    }

    private boolean shouldPresent() {
        if (!preferenceEnabled || !sessionEligible) {
            return false;
        }
        if (suppressUntilUnlocked.get()) {
            return false;
        }
        if (screenOff.get() || !isScreenInteractive()) {
            return false;
        }
        return isKeyguardLocked();
    }

    private boolean isKeyguardLocked() {
        KeyguardManager km = (KeyguardManager) appContext.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }

    private boolean isScreenInteractive() {
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isInteractive();
    }

    private boolean tryStartActivityDirect() {
        try {
            Intent intent = buildTimerIntent();
            appContext.startActivity(intent);
            return true;
        } catch (Exception e) {
            AppLog.d(TAG, "Direct startActivity failed: " + e.getMessage());
            return false;
        }
    }

    @NonNull
    private Intent buildTimerIntent() {
        return LockScreenTimerActivity.createIntent(appContext);
    }

    private void fireFullScreenIntentNotification() {
        ensureChannel();
        Intent intent = buildTimerIntent();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent fullScreen = PendingIntent.getActivity(appContext, 70, intent, flags);
        PendingIntent content = PendingIntent.getActivity(appContext, 71, intent, flags);

        // highPriority=false：屏已亮时盖住即可，避免被系统当作唤醒闹钟
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID_LOCK_PRESENT)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(appContext.getString(R.string.lock_screen_present_notification_title))
                .setContentText(appContext.getString(R.string.lock_screen_present_notification_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(content)
                .setFullScreenIntent(fullScreen, false);

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID_LOCK_PRESENT, builder.build());
        } catch (SecurityException e) {
            AppLog.w(TAG, "Failed to post lock present FSI notification", e);
        }
    }

    private void cancelPresentNotification() {
        try {
            NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID_LOCK_PRESENT);
        } catch (Exception ignored) {
        }
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID_LOCK_PRESENT);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_LOCK_PRESENT,
                appContext.getString(R.string.timer_channel_lock_present_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(appContext.getString(R.string.timer_channel_lock_present_description));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }
}
