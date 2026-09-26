package com.skyinit.pomodorotimer.ui.home;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.LockScreenPresenter;

/**
 * 锁屏专用计时页：仅展示倒计时 + 长按解锁，不提供暂停/停止等操作。
 * 与 {@link TimerActivity} 完全分离，避免污染正常计时交互。
 */
public class LockScreenTimerActivity extends AppCompatActivity {

    private static final long LONG_PRESS_MS = 900L;

    private TextView timerText;
    private TextView taskTitleText;
    private TextView sessionHintText;
    private ImageView bgImageView;
    private View unlockContainer;
    private ProgressBar unlockProgress;

    private TimerService timerService;
    private TimerViewModel viewModel;
    private boolean isBound;
    private boolean longPressTracking;
    private long longPressStartElapsed;
    private boolean unlocking;

    private final Runnable progressTicker = this::tickLongPress;

    private final BroadcastReceiver endReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.ACTION_ACTIVITY_ENDED_BROADCAST.equals(intent.getAction())) {
                finish();
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            isBound = true;
            if (!timerService.isLockScreenFullscreenEnabled()
                    || !(timerService.isRunning() || timerService.isPaused())) {
                finish();
                return;
            }
            viewModel.syncFromService(timerService);
            render(viewModel.getTimerState().getValue());
            timerService.notifyLockScreenUiResumed();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            timerService = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLockWindowFlags();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_lock_screen_timer);
        applyWindowInsets();

        viewModel = new ViewModelProvider(
                this,
                ((App) getApplication()).getContainer().getViewModelFactory()
        ).get(TimerViewModel.class);
        viewModel.getTimerState().observe(this, this::render);

        timerText = findViewById(R.id.timer_text);
        taskTitleText = findViewById(R.id.task_title_text);
        sessionHintText = findViewById(R.id.session_hint_text);
        bgImageView = findViewById(R.id.bg_image);
        unlockContainer = findViewById(R.id.lock_unlock_container);
        unlockProgress = findViewById(R.id.lock_unlock_progress);
        TimerBackgroundHelper.applyRandomBackground(this, bgImageView);
        bindUnlockGesture();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 锁屏仅展示：忽略返回
            }
        });

        IntentFilter filter = new IntentFilter(TimerService.ACTION_ACTIVITY_ENDED_BROADCAST);
        ContextCompat.registerReceiver(this, endReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        Intent serviceIntent = new Intent(this, TimerService.class);
        TimerServiceLauncher.ensureRunning(this);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLockWindowFlags();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (timerService != null) {
            timerService.notifyLockScreenUiResumed();
        }
        // 已解锁且非主动长按解锁流程：关闭本页，避免盖住正常计时页
        if (!unlocking && !isKeyguardLocked() && timerService != null
                && (timerService.isRunning() || timerService.isPaused())) {
            finish();
        }
    }

    @Override
    protected void onPause() {
        // 灭屏/离开时清掉 TURN_SCREEN_ON 与 KEEP_SCREEN_ON，避免 OEM 因窗口 flag 再次唤醒
        clearWakeFlags();
        super.onPause();
    }

    @Override
    protected void onStop() {
        boolean configChange = isChangingConfigurations();
        if (timerService != null) {
            timerService.notifyLockScreenUiStopped(configChange);
        }
        clearWakeFlags();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cancelLongPress();
        TimerBackgroundHelper.recycle(bgImageView);
        bgImageView = null;
        try {
            unregisterReceiver(endReceiver);
        } catch (Exception e) {
            AppLog.w("LockScreenTimer", "Receiver already unregistered", e);
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        clearLockWindowFlags();
        super.onDestroy();
    }

    private void render(@Nullable TimerUiState state) {
        if (state == null) {
            return;
        }
        if (!state.running && !state.paused) {
            finish();
            return;
        }
        timerText.setText(viewModel.formatTime(state.timeLeftMillis));
        if (taskTitleText != null && timerService != null) {
            String title = timerService.getCurrentTaskTitle();
            if (title != null && !title.isEmpty()) {
                taskTitleText.setVisibility(View.VISIBLE);
                taskTitleText.setText(getString(R.string.timer_current_task, title));
            } else {
                taskTitleText.setVisibility(View.GONE);
            }
        }
        if (sessionHintText != null) {
            if (state.paused) {
                TimerScreenUiState screen = TimerScreenUiState.from(
                        state, viewModel::formatTime, this);
                sessionHintText.setVisibility(View.VISIBLE);
                sessionHintText.setText(screen.pauseHintText);
                if (screen.pauseHintUrgent) {
                    sessionHintText.setTextColor(
                            ContextCompat.getColor(this, R.color.semantic_warning));
                } else {
                    sessionHintText.setTextColor(0x99FFFFFF);
                }
            } else {
                int hintRes = viewModel.resolveSessionHintResId(state);
                if (hintRes != 0) {
                    sessionHintText.setVisibility(View.VISIBLE);
                    sessionHintText.setText(hintRes);
                    sessionHintText.setTextColor(0x99FFFFFF);
                } else {
                    sessionHintText.setVisibility(View.GONE);
                }
            }
        }
    }

    private void bindUnlockGesture() {
        if (unlockContainer == null) {
            return;
        }
        unlockContainer.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    beginLongPress();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (longPressTracking) {
                        long elapsed = SystemClock.elapsedRealtime() - longPressStartElapsed;
                        if (elapsed >= LONG_PRESS_MS) {
                            completeUnlock();
                        } else {
                            cancelLongPress();
                        }
                    }
                    return true;
                default:
                    return true;
            }
        });
    }

    private void beginLongPress() {
        longPressTracking = true;
        longPressStartElapsed = SystemClock.elapsedRealtime();
        if (unlockProgress != null) {
            unlockProgress.setProgress(0);
        }
        unlockContainer.post(progressTicker);
    }

    private void tickLongPress() {
        if (!longPressTracking) {
            return;
        }
        long elapsed = SystemClock.elapsedRealtime() - longPressStartElapsed;
        int pct = (int) Math.min(100, (elapsed * 100) / LONG_PRESS_MS);
        if (unlockProgress != null) {
            unlockProgress.setProgress(pct);
        }
        if (elapsed >= LONG_PRESS_MS) {
            completeUnlock();
            return;
        }
        unlockContainer.postDelayed(progressTicker, 16L);
    }

    private void completeUnlock() {
        cancelLongPress();
        unlocking = true;
        if (timerService != null) {
            timerService.notifyUserRequestedUnlock();
        }
        clearLockWindowFlags();
        // 结束锁屏专用页，露出系统锁屏；不 dismiss keyguard
        finish();
    }

    private void cancelLongPress() {
        longPressTracking = false;
        if (unlockContainer != null) {
            unlockContainer.removeCallbacks(progressTicker);
        }
        if (unlockProgress != null) {
            unlockProgress.setProgress(0);
        }
    }

    private void applyLockWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            // 仅在屏已亮时用于盖住锁屏；熄屏路径不会走到本页拉起
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }

    private void clearWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(false);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void clearLockWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false);
            setTurnScreenOn(false);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private boolean isKeyguardLocked() {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }

    private void applyWindowInsets() {
        View content = findViewById(R.id.timer_content);
        if (content == null) {
            return;
        }
        final int initialLeft = content.getPaddingLeft();
        final int initialTop = content.getPaddingTop();
        final int initialRight = content.getPaddingRight();
        final int initialBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    initialLeft + bars.left,
                    initialTop + bars.top,
                    initialRight + bars.right,
                    initialBottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, LockScreenTimerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(LockScreenPresenter.EXTRA_LOCK_PRESENTATION, true);
        return intent;
    }
}
