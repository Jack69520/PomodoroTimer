package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.R;
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.util.Log;
import androidx.activity.OnBackPressedCallback;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.res.Configuration;

/**
 * 计时器专属页面：仅包含计时显示与控制按钮。
 */
public class TimerActivity extends AppCompatActivity implements PauseReasonDialog.PauseReasonListener {
    private static final int[] TIMER_BACKGROUND_RES_IDS = {
            R.drawable.timerbackgroundimage_1,
            R.drawable.timerbackgroundimage_2,
            R.drawable.timerbackgroundimage_3,
            R.drawable.timerbackgroundimage_4,
            R.drawable.timerbackgroundimage_5,
            R.drawable.timerbackgroundimage_6,
            R.drawable.timerbackgroundimage_7,
            R.drawable.timerbackgroundimage_8,
            R.drawable.timerbackgroundimage_9,
            R.drawable.timerbackgroundimage_10,
            R.drawable.timerbackgroundimage_11,
            R.drawable.timerbackgroundimage_12
    };

    private TextView timerText;
    private TextView taskTitleText;
    private TextView sessionHintText;
    private Button primaryButton;
    private Button secondaryButton;
    private ImageView bgImageView;

    private TimerService timerService;
    private TimerViewModel viewModel;
    private boolean isBound = false;
    private final BroadcastReceiver endReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.ACTION_ACTIVITY_ENDED_BROADCAST.equals(intent.getAction())) {
                Toast.makeText(TimerActivity.this, getString(R.string.timer_toast_activity_ended), Toast.LENGTH_SHORT).show();
                navigateHome();
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            isBound = true;
            applyTaskFromIntent();
            maybeStartOnEnter();
            viewModel.syncFromService(timerService);
            updateUIFromState(viewModel.getTimerState().getValue());
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
        setContentView(R.layout.activity_timer);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(TimerViewModel.class);
        viewModel.getTimerState().observe(this, this::updateUIFromState);

        timerText = findViewById(R.id.timer_text);
        taskTitleText = findViewById(R.id.task_title_text);
        sessionHintText = findViewById(R.id.session_hint_text);
        bgImageView = findViewById(R.id.bg_image);
        primaryButton = findViewById(R.id.primary_button);
        secondaryButton = findViewById(R.id.secondary_button);

        // 绑定服务
        Intent serviceIntent = new Intent(this, TimerService.class);
        TimerServiceLauncher.ensureRunning(this);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        // 随机背景：进入页面时尝试设置图片背景（失败则回退为渐变）
        maybeSetRandomBackgroundImage();

        // 统一拦截系统返回键（包括手势返回）
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!confirmExitIfRunning()) {
                    finish();
                }
            }
        });

        // 注册“活动结束”广播（Android 13+ 需声明导出标志）
        IntentFilter filter = new IntentFilter(TimerService.ACTION_ACTIVITY_ENDED_BROADCAST);
        ContextCompat.registerReceiver(this, endReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        primaryButton.setOnClickListener(v -> {
            if (timerService == null) return;
            TimerUiState state = viewModel.getTimerState().getValue();
            if (state != null && state.awaitingPostBreakChoice) {
                sendAction(TimerService.ACTION_START);
                navigateStay();
                return;
            }
            if (timerService.isRunning()) {
                if (timerService.canPause()) {
                    PauseReasonDialog dialog = new PauseReasonDialog();
                    dialog.show(getSupportFragmentManager(), "pause_reason_dialog");
                } else {
                    Toast.makeText(this, getString(R.string.timer_toast_max_pause_reached), Toast.LENGTH_SHORT).show();
                }
            } else if (timerService.isPaused()) {
                sendAction(TimerService.ACTION_RESUME);
            } else {
                sendAction(TimerService.ACTION_START);
            }
        });

        secondaryButton.setOnClickListener(v -> {
            if (timerService == null) return;
            TimerUiState state = viewModel.getTimerState().getValue();
            if (state != null && state.isBreakSession() && state.running) {
                sendAction(TimerService.ACTION_END_BREAK);
                vibrateShort();
                Toast.makeText(this, getString(R.string.timer_toast_break_ended), Toast.LENGTH_SHORT).show();
                navigateHome();
                return;
            }
            if (state != null && state.awaitingPostBreakChoice) {
                sendAction(TimerService.ACTION_RESET);
                vibrateShort();
                Toast.makeText(this, getString(R.string.timer_toast_activity_ended), Toast.LENGTH_SHORT).show();
                navigateHome();
                return;
            }
            sendAction(TimerService.ACTION_RESET);
            vibrateShort();
            Toast.makeText(this, getString(R.string.timer_toast_timer_ended), Toast.LENGTH_SHORT).show();
            navigateHome();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(endReceiver); } catch (Exception ignored) {}
        if (bgImageView != null) {
            try {
                android.graphics.drawable.Drawable drawable = bgImageView.getDrawable();
                bgImageView.setImageDrawable(null);
                if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                    android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            } catch (Exception ignored) {}
        }
        if (timerService != null) {
            viewModel.syncFromService(timerService);
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    private void sendAction(String action) {
        TimerServiceLauncher.deliverAction(this, action);
    }

    private void applyTaskFromIntent() {
        Intent intent = getIntent();
        if (timerService == null || intent == null) {
            return;
        }
        if (intent.hasExtra(TimerService.EXTRA_TASK_ID)) {
            timerService.setCurrentTask(
                    intent.getIntExtra(TimerService.EXTRA_TASK_ID, -1),
                    intent.getStringExtra(TimerService.EXTRA_TASK_TITLE),
                    intent.getStringExtra(TimerService.EXTRA_TASK_CATEGORY),
                    intent.getStringExtra(TimerService.EXTRA_TASK_TAGS),
                    intent.getIntExtra(TimerService.EXTRA_SUB_TASK_ID, -1)
            );
        }
    }

    private void maybeStartOnEnter() {
        boolean start = getIntent().getBooleanExtra("start", false);
        if (start && timerService != null && !timerService.isRunning() && !timerService.isPaused()) {
            sendAction(TimerService.ACTION_START);
        }
    }

    private void navigateHome() {
        Intent home = new Intent(this, MainActivity.class);
        home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }

    private void navigateStay() {
        updateUIFromState(viewModel.getTimerState().getValue());
    }

    private void vibrateShort() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(80);
                }
            }
        } catch (Exception ignored) {}
    }

    private void updateUIFromState(TimerUiState state) {
        if (state == null) {
            if (timerService != null) {
                viewModel.syncFromService(timerService);
            }
            return;
        }
        timerText.setText(viewModel.formatTime(state.timeLeftMillis));
        updateSessionLabels(state);
        updateControls(state);
    }

    private void updateSessionLabels(TimerUiState state) {
        if (timerService != null && taskTitleText != null) {
            String title = timerService.getCurrentTaskTitle();
            if (title != null && !title.isEmpty()) {
                taskTitleText.setVisibility(View.VISIBLE);
                taskTitleText.setText(getString(R.string.timer_current_task, title));
            } else {
                taskTitleText.setVisibility(View.GONE);
            }
        }
        if (sessionHintText != null && state != null) {
            int hintRes = viewModel.resolveSessionHintResId(state);
            if (hintRes != 0) {
                sessionHintText.setVisibility(View.VISIBLE);
                sessionHintText.setText(hintRes);
            } else {
                sessionHintText.setVisibility(View.GONE);
            }
        }
    }

    private void updateControls(TimerUiState state) {
        if (state == null) {
            return;
        }
        if (viewModel.shouldShowPostBreakActions(state)) {
            primaryButton.setText(R.string.timer_start_next_pomodoro);
            primaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText(R.string.timer_end_session);
            secondaryButton.setVisibility(View.VISIBLE);
            return;
        }
        if (viewModel.shouldShowBreakEndButton(state)) {
            primaryButton.setVisibility(View.GONE);
            secondaryButton.setText(R.string.timer_end_break);
            secondaryButton.setVisibility(View.VISIBLE);
            return;
        }
        if (state.running) {
            primaryButton.setText(R.string.pause_button);
            primaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText(R.string.stop_button);
            secondaryButton.setVisibility(View.VISIBLE);
        } else if (state.paused) {
            primaryButton.setText(R.string.resume_button);
            primaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText(R.string.stop_button);
            secondaryButton.setVisibility(View.VISIBLE);
        } else if (viewModel.shouldShowIdleStudyControls(state)) {
            primaryButton.setText(R.string.start_button);
            primaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText(R.string.stop_button);
            secondaryButton.setVisibility(View.VISIBLE);
        } else {
            primaryButton.setVisibility(View.GONE);
            secondaryButton.setVisibility(View.GONE);
        }
    }

    private void maybeSetRandomBackgroundImage() {
        if (bgImageView == null || TIMER_BACKGROUND_RES_IDS.length == 0) {
            return;
        }
        try {
            int resId = TIMER_BACKGROUND_RES_IDS[new java.util.Random().nextInt(TIMER_BACKGROUND_RES_IDS.length)];
            if (loadBackgroundImage(resId)) {
                bgImageView.setVisibility(View.VISIBLE);
            } else {
                Log.w("TimerActivity", "Failed to load timer background, using gradient fallback. resId=" + resId);
                bgImageView.setImageDrawable(null);
                bgImageView.setVisibility(View.GONE);
            }
        } catch (Throwable t) {
            Log.e("TimerActivity", "Error setting timer background, using gradient fallback.", t);
            bgImageView.setImageDrawable(null);
            bgImageView.setVisibility(View.GONE);
        }
    }

    /**
     * 使用静态资源 ID + ImageDecoder 加载 WebP 背景，避免 getIdentifier 在资源收缩后失效，
     * 以及 BitmapFactory 对 WebP 做 bounds 预检时常失败的问题。
     */
    private boolean loadBackgroundImage(int resId) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int targetW = Math.max(dm.widthPixels, 1);
        int targetH = Math.max(dm.heightPixels, 1);
        try {
            android.graphics.ImageDecoder.Source source =
                    android.graphics.ImageDecoder.createSource(getResources(), resId);
            android.graphics.Bitmap bitmap = android.graphics.ImageDecoder.decodeBitmap(source,
                    (decoder, info, src) -> {
                        decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                        int iw = info.getSize().getWidth();
                        int ih = info.getSize().getHeight();
                        if (iw <= 0 || ih <= 0) {
                            return;
                        }
                        float scale = Math.min((float) targetW / iw, (float) targetH / ih);
                        if (scale < 1f) {
                            decoder.setTargetSize(
                                    Math.max(1, Math.round(iw * scale)),
                                    Math.max(1, Math.round(ih * scale))
                            );
                        }
                    });
            if (bitmap != null) {
                bgImageView.setImageBitmap(bitmap);
                return true;
            }
        } catch (Exception e) {
            Log.w("TimerActivity", "ImageDecoder failed for resId=" + resId, e);
        }
        try {
            bgImageView.setImageResource(resId);
            return bgImageView.getDrawable() != null;
        } catch (Exception e) {
            Log.w("TimerActivity", "setImageResource failed for resId=" + resId, e);
            return false;
        }
    }

    // 暂停原因选择回调
    @Override
    public void onReasonSelected(String reason) {
        Intent intent = new Intent(this, TimerService.class);
        intent.setAction(TimerService.ACTION_PAUSE_WITH_REASON);
        intent.putExtra("pause_reason", reason);
        TimerServiceLauncher.deliverAction(this, intent);
    }

    @Override
    public void resumeTimer() {
        TimerServiceLauncher.deliverAction(this, TimerService.ACTION_RESUME);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 允许后台继续运行（例如打开白名单应用），不再强制失败
    }

    @Override
    public void onBackPressed() {
        if (!confirmExitIfRunning()) {
            super.onBackPressed();
        }
    }

    private boolean confirmExitIfRunning() {
        if (timerService != null && timerService.isRunning() && timerService.getSessionType() == 0) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.timer_dialog_exit_title)
                    .setMessage(R.string.timer_dialog_exit_message)
                    .setPositiveButton(R.string.timer_dialog_exit_confirm, (d, w) -> {
                        sendAction(TimerService.ACTION_RESET);
                        Toast.makeText(this, getString(R.string.timer_toast_activity_ended), Toast.LENGTH_SHORT).show();
                        navigateHome();
                    })
                    .setNegativeButton(R.string.timer_dialog_exit_continue, (d, w) -> d.dismiss())
                    .setCancelable(true)
                    .show();
            return true;
        }
        return false;
    }
}


