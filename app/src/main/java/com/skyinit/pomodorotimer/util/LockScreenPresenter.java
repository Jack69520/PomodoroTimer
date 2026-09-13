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
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.home.LockScreenTimerActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 锁屏全屏计时强拉回调度：混合亮屏（不在 SCREEN_OFF 唤醒）、
 * suppressUntilUnlocked 防解锁竞态、独立 FSI 通知（不改动 ongoing Chronometer）。
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
        // 仅在「变为可展示」且当前无任何计时 UI 在前台时拉起
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
        if (shouldPresent()) {
            tryPresent("normal_ui_stopped");
        }
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
        if (dueToConfigChange) {
            return;
        }
        // 长按解锁后的 stop 不应立刻再拉起（由 suppressUntilUnlocked 拦截）
        if (shouldPresent()) {
            tryPresent("lock_ui_stopped");
        }
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
     * 用户灭屏后再次点亮：若已处于锁屏，开启新一轮展示（清除长按解锁抑制）。
     * 不强行在 SCREEN_OFF 唤醒。
     */
    @MainThread
    public void onScreenOn() {
        if (isKeyguardLocked()) {
            // 再次进入锁屏亮屏周期：允许重新全屏展示
            // （USER_UNLOCKED/PRESENT 在部分机型上可能收不到，不能只依赖它们清 suppress）
            suppressUntilUnlocked.set(false);
        }
        tryPresent("screen_on");
    }

    /** 灭屏：结束当前展示抑制，下一轮亮屏+锁屏可再展示。 */
    @MainThread
    public void onScreenOff() {
        // 不在此处 tryPresent；仅清除抑制与防抖，避免长按解锁后同屏立刻盖回，
        // 同时保证「解锁 → 再灭屏 → 再亮屏」不被 1.5s debounce 误伤。
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
        return preferenceEnabled
                && sessionEligible
                && !suppressUntilUnlocked.get()
                && isKeyguardLocked();
    }

    private boolean isKeyguardLocked() {
        KeyguardManager km = (KeyguardManager) appContext.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID_LOCK_PRESENT)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(appContext.getString(R.string.lock_screen_present_notification_title))
                .setContentText(appContext.getString(R.string.lock_screen_present_notification_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(content)
                .setFullScreenIntent(fullScreen, true);

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
