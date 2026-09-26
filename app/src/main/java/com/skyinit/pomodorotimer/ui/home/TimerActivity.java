package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.domain.timer.PauseReasonPromptMode;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.AppLog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;

/**
 * 计时器专属页面：仅包含计时显示与控制按钮。
 */
public class TimerActivity extends AppCompatActivity implements PauseReasonDialog.PauseReasonListener {

    private TextView timerText;
    private TextView taskTitleText;
    private TextView sessionHintText;
    private Button primaryButton;
    private Button secondaryButton;
    private ImageView bgImageView;

    private TimerService timerService;
    private TimerViewModel viewModel;
    private boolean isBound = false;
    private int defaultHintColor = Color.WHITE;

    private final BroadcastReceiver endReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.ACTION_ACTIVITY_ENDED_BROADCAST.equals(intent.getAction())) {
                Toast.makeText(TimerActivity.this,
                        getString(R.string.timer_toast_activity_ended), Toast.LENGTH_SHORT).show();
                navigateHome();
            }
        }
    };

    private void applyTimerWindowInsets() {
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
            timerService.notifyTimerUiResumed();
            viewModel.dispatch(TimerIntent.serviceBound());
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_timer);
        applyTimerWindowInsets();

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(TimerViewModel.class);
        viewModel.getTimerState().observe(this, this::updateUIFromState);
        viewModel.getScreenState().observe(this, this::applyScreenState);
        viewModel.getEffects().observe(this, this::handleEffect);

        timerText = findViewById(R.id.timer_text);
        taskTitleText = findViewById(R.id.task_title_text);
        sessionHintText = findViewById(R.id.session_hint_text);
        bgImageView = findViewById(R.id.bg_image);
        primaryButton = findViewById(R.id.primary_button);
        secondaryButton = findViewById(R.id.secondary_button);
        if (sessionHintText != null) {
            defaultHintColor = sessionHintText.getCurrentTextColor();
        }

        Intent serviceIntent = new Intent(this, TimerService.class);
        TimerServiceLauncher.ensureRunning(this);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        TimerBackgroundHelper.applyRandomBackground(this, bgImageView);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                TimerUiState state = viewModel.getTimerState().getValue();
                if (viewModel.shouldConfirmExit(state)) {
                    showExitConfirm();
                } else {
                    finish();
                }
            }
        });

        IntentFilter filter = new IntentFilter(TimerService.ACTION_ACTIVITY_ENDED_BROADCAST);
        ContextCompat.registerReceiver(this, endReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        primaryButton.setOnClickListener(v -> viewModel.dispatch(TimerIntent.primaryClicked()));
        secondaryButton.setOnClickListener(v -> viewModel.dispatch(TimerIntent.secondaryClicked()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (timerService != null) {
            timerService.notifyTimerUiResumed();
        }
        viewModel.dispatch(TimerIntent.screenResumed());
    }

    @Override
    protected void onStop() {
        if (timerService != null) {
            timerService.notifyTimerUiStopped(isChangingConfigurations());
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(endReceiver);
        } catch (Exception e) {
            AppLog.w("TimerActivity", "Receiver already unregistered", e);
        }
        if (bgImageView != null) {
            TimerBackgroundHelper.recycle(bgImageView);
            bgImageView = null;
        }
        if (timerService != null) {
            viewModel.syncFromService(timerService);
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    private void handleEffect(@Nullable TimerEffect effect) {
        if (effect == null || isFinishing()) {
            return;
        }
        switch (effect.type) {
            case DELIVER_ACTION:
                if (effect.serviceAction != null) {
                    TimerServiceLauncher.deliverAction(this, effect.serviceAction);
                }
                break;
            case DELIVER_ACTION_WITH_REASON:
                if (effect.serviceAction != null && effect.pauseReason != null) {
                    Intent intent = new Intent(this, TimerService.class);
                    intent.setAction(effect.serviceAction);
                    intent.putExtra(TimerService.EXTRA_PAUSE_REASON, effect.pauseReason);
                    TimerServiceLauncher.deliverAction(this, intent);
                }
                break;
            case SHOW_TOAST:
                Toast.makeText(this, effect.toastRes, Toast.LENGTH_SHORT).show();
                break;
            case SHOW_PAUSE_REASON_DIALOG:
                showPauseReasonDialog(effect.promptMode != null
                        ? effect.promptMode
                        : PauseReasonPromptMode.ASK_SKIPPABLE);
                break;
            case DISMISS_PAUSE_REASON_DIALOG:
                dismissPauseReasonDialog();
                break;
            case SHOW_STOP_CONFIRM:
                showStopConfirm();
                break;
            case SHOW_END_BREAK_CONFIRM:
                showEndBreakConfirm();
                break;
            case SHOW_EXIT_CONFIRM:
                showExitConfirm();
                break;
            case FINISH_SESSION:
                if (effect.serviceAction != null) {
                    TimerServiceLauncher.deliverAction(this, effect.serviceAction);
                }
                vibrateShort();
                if (effect.toastRes != 0) {
                    Toast.makeText(this, effect.toastRes, Toast.LENGTH_SHORT).show();
                }
                navigateHome();
                break;
            default:
                break;
        }
    }

    private void showPauseReasonDialog(PauseReasonPromptMode mode) {
        if (getSupportFragmentManager().findFragmentByTag(PauseReasonDialog.TAG) != null) {
            return;
        }
        PauseReasonDialog.newInstance(mode)
                .show(getSupportFragmentManager(), PauseReasonDialog.TAG);
    }

    private void dismissPauseReasonDialog() {
        androidx.fragment.app.Fragment existing =
                getSupportFragmentManager().findFragmentByTag(PauseReasonDialog.TAG);
        if (existing instanceof PauseReasonDialog) {
            ((PauseReasonDialog) existing).dismissAllowingStateLoss();
        }
    }

    private void showStopConfirm() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.timer_confirm_reset_title)
                .setMessage(R.string.timer_confirm_reset_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.stop_button, (d, w) ->
                        viewModel.dispatch(TimerIntent.confirmStop()))
                .show();
    }

    private void showEndBreakConfirm() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.timer_confirm_end_break_title)
                .setMessage(R.string.timer_confirm_end_break_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.timer_end_break, (d, w) ->
                        viewModel.dispatch(TimerIntent.confirmEndBreak()))
                .show();
    }

    private void showExitConfirm() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.timer_dialog_exit_title)
                .setMessage(R.string.timer_dialog_exit_message)
                .setPositiveButton(R.string.timer_dialog_exit_confirm, (d, w) ->
                        viewModel.dispatch(TimerIntent.confirmExitReset()))
                .setNegativeButton(R.string.timer_dialog_exit_continue, (d, w) -> d.dismiss())
                .setCancelable(true)
                .show();
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
            TimerServiceLauncher.deliverAction(this, TimerService.ACTION_START);
        }
    }

    private void navigateHome() {
        Intent home = new Intent(this, MainActivity.class);
        home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
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
        } catch (Exception e) {
            AppLog.w("TimerActivity", "Vibration failed", e);
        }
    }

    private void applyScreenState(@Nullable TimerScreenUiState screen) {
        if (sessionHintText == null || screen == null) {
            return;
        }
        if (screen.pauseHintVisible && screen.pauseHintText != null) {
            sessionHintText.setVisibility(View.VISIBLE);
            sessionHintText.setText(screen.pauseHintText);
            if (screen.pauseHintUrgent) {
                sessionHintText.setTextColor(ContextCompat.getColor(this, R.color.semantic_warning));
            } else {
                sessionHintText.setTextColor(defaultHintColor);
            }
        }
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
        if (sessionHintText != null && state != null && !state.paused) {
            int hintRes = viewModel.resolveSessionHintResId(state);
            if (hintRes != 0) {
                sessionHintText.setVisibility(View.VISIBLE);
                sessionHintText.setText(hintRes);
                sessionHintText.setTextColor(defaultHintColor);
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
            primaryButton.setEnabled(true);
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
            if (state.isStudySession() && !state.canPause) {
                primaryButton.setVisibility(View.GONE);
            } else {
                primaryButton.setText(R.string.pause_button);
                primaryButton.setEnabled(true);
                primaryButton.setVisibility(View.VISIBLE);
            }
            secondaryButton.setText(R.string.stop_button);
            secondaryButton.setVisibility(View.VISIBLE);
        } else if (state.paused) {
            primaryButton.setText(R.string.resume_button);
            primaryButton.setEnabled(true);
            primaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText(R.string.stop_button);
            secondaryButton.setVisibility(View.VISIBLE);
        } else if (viewModel.shouldShowIdleStudyControls(state)) {
            primaryButton.setText(R.string.start_button);
            primaryButton.setEnabled(true);
            primaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText(R.string.stop_button);
            secondaryButton.setVisibility(View.VISIBLE);
        } else {
            primaryButton.setVisibility(View.GONE);
            secondaryButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onReasonSelected(String reason) {
        viewModel.dispatch(TimerIntent.reasonPicked(reason));
    }

    @Override
    public void onReasonSkipped() {
        viewModel.dispatch(TimerIntent.reasonSkipped());
    }

    @Override
    public void onReasonQuickResume() {
        viewModel.dispatch(TimerIntent.reasonQuickResume());
    }
}
