package com.skyinit.pomodorotimer.ui.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;
import com.skyinit.pomodorotimer.util.FocusDndHelper;
import com.skyinit.pomodorotimer.util.LockScreenTimerGate;
import com.skyinit.pomodorotimer.util.StudyDurationPickerHelper;

/**
 * 番茄钟与待办设置子页：分组列表 + 对话框取值，避免 Spinner 误触。
 */
public class PomodoroSettingsActivity extends SubpageActivity {

    private PomodoroSettingsViewModel viewModel;

    private TextView studyDurationValue;
    private TextView breakDurationValue;
    private TextView maxPauseValue;
    private TextView longBreakIntervalValue;
    private TextView longBreakDurationValue;
    private View longBreakDetails;
    private SwitchCompat autoStartSwitch;
    private SwitchCompat longBreakSwitch;
    private SwitchCompat dndSwitch;
    private SwitchCompat autoBlockSwitch;
    private SwitchCompat lockScreenSwitch;
    private SwitchCompat autoDeleteSwitch;
    private SwitchCompat collectionProgressDetailsSwitch;

    private boolean suppressSwitchCallbacks;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_pomodoro_settings, R.string.settings_pomodoro_entry_title);

        viewModel = new ViewModelProvider(
                this,
                ((App) getApplication()).getContainer().getViewModelFactory()
        ).get(PomodoroSettingsViewModel.class);

        bindViews();
        bindClicks();
        observeVm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.onHostResumed();
    }

    private void bindViews() {
        studyDurationValue = findViewById(R.id.study_duration_value);
        breakDurationValue = findViewById(R.id.break_duration_value);
        maxPauseValue = findViewById(R.id.max_pause_value);
        longBreakIntervalValue = findViewById(R.id.long_break_interval_value);
        longBreakDurationValue = findViewById(R.id.long_break_duration_value);
        longBreakDetails = findViewById(R.id.long_break_details);
        autoStartSwitch = findViewById(R.id.auto_start_after_break_switch);
        longBreakSwitch = findViewById(R.id.long_break_enabled_switch);
        dndSwitch = findViewById(R.id.dnd_during_focus_switch);
        autoBlockSwitch = findViewById(R.id.auto_block_during_pomodoro_switch);
        lockScreenSwitch = findViewById(R.id.lock_screen_fullscreen_switch);
        autoDeleteSwitch = findViewById(R.id.auto_delete_switch);
        collectionProgressDetailsSwitch = findViewById(R.id.collection_progress_details_switch);
    }

    private void bindClicks() {
        findViewById(R.id.row_study_duration).setOnClickListener(v ->
                viewModel.dispatch(PomodoroSettingsIntent.openStudyDurationPicker()));
        findViewById(R.id.row_break_duration).setOnClickListener(v ->
                viewModel.dispatch(PomodoroSettingsIntent.openBreakDurationPicker()));
        findViewById(R.id.row_max_pause).setOnClickListener(v ->
                viewModel.dispatch(PomodoroSettingsIntent.openPauseCountPicker()));
        findViewById(R.id.row_long_break_interval).setOnClickListener(v ->
                viewModel.dispatch(PomodoroSettingsIntent.openLongBreakIntervalPicker()));
        findViewById(R.id.row_long_break_duration).setOnClickListener(v ->
                viewModel.dispatch(PomodoroSettingsIntent.openLongBreakDurationPicker()));

        autoStartSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallbacks) {
                return;
            }
            viewModel.dispatch(PomodoroSettingsIntent.setAutoStart(isChecked));
        });
        longBreakSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallbacks) {
                return;
            }
            viewModel.dispatch(PomodoroSettingsIntent.setLongBreakEnabled(isChecked));
        });
        dndSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallbacks) {
                return;
            }
            viewModel.dispatch(PomodoroSettingsIntent.setDnd(isChecked));
        });
        autoBlockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallbacks) {
                return;
            }
            viewModel.dispatch(PomodoroSettingsIntent.setAutoBlock(isChecked));
        });
        lockScreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallbacks) {
                return;
            }
            viewModel.dispatch(PomodoroSettingsIntent.setLockScreenFullscreen(isChecked));
        });
        autoDeleteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallbacks) {
                return;
            }
            viewModel.dispatch(PomodoroSettingsIntent.setAutoDelete(isChecked));
        });
        collectionProgressDetailsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallbacks) {
                return;
            }
            viewModel.dispatch(PomodoroSettingsIntent.setCollectionProgressDetails(isChecked));
        });
    }

    private void observeVm() {
        viewModel.getUiState().observe(this, this::render);
        viewModel.getEffects().observe(this, this::handleEffect);
    }

    private void render(@Nullable PomodoroSettingsUiState state) {
        if (state == null || isFinishing() || isDestroyed()) {
            return;
        }
        studyDurationValue.setText(state.studyDurationLabel);
        breakDurationValue.setText(state.breakDurationLabel);
        maxPauseValue.setText(state.maxPauseLabel);
        longBreakIntervalValue.setText(state.longBreakIntervalLabel);
        longBreakDurationValue.setText(state.longBreakDurationLabel);
        longBreakDetails.setVisibility(state.longBreakEnabled ? View.VISIBLE : View.GONE);

        suppressSwitchCallbacks = true;
        autoStartSwitch.setChecked(state.autoStartAfterBreak);
        longBreakSwitch.setChecked(state.longBreakEnabled);
        dndSwitch.setChecked(state.dndEnabled);
        autoBlockSwitch.setChecked(state.autoBlockDuringPomodoro);
        lockScreenSwitch.setChecked(state.lockScreenFullscreenEnabled);
        autoDeleteSwitch.setChecked(state.autoDeleteCompleted);
        collectionProgressDetailsSwitch.setChecked(state.collectionProgressDetails);
        suppressSwitchCallbacks = false;
    }

    private void handleEffect(@Nullable PomodoroSettingsEffect effect) {
        if (effect == null || isFinishing() || isDestroyed()) {
            return;
        }
        switch (effect.type) {
            case SHOW_TOAST:
                Toast.makeText(this, effect.toastRes, Toast.LENGTH_SHORT).show();
                break;
            case SHOW_STUDY_DURATION_PICKER:
                StudyDurationPickerHelper.show(
                        this,
                        getString(R.string.settings_study_duration),
                        getString(R.string.confirm),
                        effect.studyTimeMs,
                        (totalMinutes, millis) ->
                                viewModel.dispatch(PomodoroSettingsIntent.setStudyDuration(millis)));
                break;
            case SHOW_SINGLE_CHOICE:
                showSingleChoice(effect.choiceKind, effect.selectedIndex);
                break;
            case SHOW_DND_PERMISSION_DIALOG:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.settings_dnd_permission_title)
                        .setMessage(R.string.settings_dnd_permission_message)
                        .setPositiveButton(R.string.confirm, (d, w) ->
                                viewModel.dispatch(PomodoroSettingsIntent.requestDndPermission()))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            case OPEN_DND_SETTINGS:
                startActivity(FocusDndHelper.createPolicyAccessIntent(this));
                break;
            case SHOW_LOCK_SCREEN_PERMISSION_DIALOG:
                showLockScreenPermissionDialog(effect.lockScreenPrecondition);
                break;
            case OPEN_FULL_SCREEN_INTENT_SETTINGS:
                startActivity(LockScreenTimerGate.createFullScreenIntentSettingsIntent(this));
                break;
            case OPEN_NOTIFICATION_SETTINGS:
                startActivity(LockScreenTimerGate.createNotificationSettingsIntent(this));
                break;
            default:
                break;
        }
    }

    private void showLockScreenPermissionDialog(
            @Nullable LockScreenTimerGate.EnablePrecondition precondition) {
        if (precondition == null) {
            return;
        }
        int messageRes;
        boolean allowDegraded = precondition == LockScreenTimerGate.EnablePrecondition.NEED_FULL_SCREEN_INTENT;
        boolean needNotification = LockScreenTimerGate.mustHaveNotification(precondition);
        switch (precondition) {
            case NEED_NOTIFICATION:
                messageRes = R.string.settings_lock_screen_permission_need_notification;
                break;
            case NEED_FULL_SCREEN_INTENT:
                messageRes = R.string.settings_lock_screen_permission_need_fsi;
                break;
            case NEED_BOTH:
                messageRes = R.string.settings_lock_screen_permission_need_both;
                break;
            default:
                return;
        }
        ModernPromptDialog.Builder builder = ModernPromptDialog.builder(this)
                .icon(R.drawable.ic_lock)
                .accent(ModernPromptDialog.Accent.BRAND)
                .title(R.string.settings_lock_screen_permission_title)
                .message(messageRes)
                .cancelable(true);
        if (needNotification) {
            builder.primary(R.string.settings_lock_screen_go_grant, () ->
                    viewModel.dispatch(PomodoroSettingsIntent.requestNotificationPermissionSettings()));
        } else {
            builder.primary(R.string.settings_lock_screen_go_grant, () ->
                    viewModel.dispatch(PomodoroSettingsIntent.requestFullScreenIntentPermission()));
        }
        if (allowDegraded) {
            builder.secondary(R.string.settings_lock_screen_enable_degraded, () ->
                    viewModel.dispatch(PomodoroSettingsIntent.confirmEnableLockScreenDegraded()));
        }
        builder.tertiary(R.string.cancel, null);
        builder.show();
    }

    private void showSingleChoice(@Nullable PomodoroSettingsEffect.ChoiceKind kind, int selectedIndex) {
        if (kind == null) {
            return;
        }
        String[] options;
        String title;
        switch (kind) {
            case BREAK_DURATION:
                options = getResources().getStringArray(R.array.break_duration_options);
                title = getString(R.string.settings_break_duration);
                break;
            case PAUSE_COUNT:
                options = getResources().getStringArray(R.array.settings_pause_count_options);
                title = getString(R.string.settings_max_pause_count);
                break;
            case LONG_BREAK_INTERVAL:
                options = getResources().getStringArray(R.array.pomodoros_before_long_break_options);
                title = getString(R.string.settings_pomodoros_before_long_break);
                break;
            case LONG_BREAK_DURATION:
                options = getResources().getStringArray(R.array.long_break_duration_options);
                title = getString(R.string.settings_long_break_duration);
                break;
            default:
                return;
        }
        int safeIndex = Math.max(0, Math.min(selectedIndex, options.length - 1));
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(options, safeIndex, (dialog, which) -> {
                    dialog.dismiss();
                    applyChoice(kind, which);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyChoice(PomodoroSettingsEffect.ChoiceKind kind, int which) {
        switch (kind) {
            case BREAK_DURATION:
                viewModel.dispatch(PomodoroSettingsIntent.setBreakDuration((which + 1L) * 60_000L));
                break;
            case PAUSE_COUNT:
                viewModel.dispatch(PomodoroSettingsIntent.setMaxPause(which + 1));
                break;
            case LONG_BREAK_INTERVAL:
                viewModel.dispatch(PomodoroSettingsIntent.setLongBreakInterval(
                        which + UserPomodoroSettings.MIN_POMODOROS_BEFORE_LONG_BREAK));
                break;
            case LONG_BREAK_DURATION:
                viewModel.dispatch(PomodoroSettingsIntent.setLongBreakDurationMinutes(10 + which));
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
