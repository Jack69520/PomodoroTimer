package com.skyinit.pomodorotimer.ui.settings;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.util.AppBlockingEnabler;

/**
 * 设置主页：现代化分组列表入口，功能细节下沉至子页。
 */
public class SettingsActivity extends SubpageActivity {

    public static final String EXTRA_ENABLE_APP_BLOCKING = "extra_enable_app_blocking";

    private SettingsHubViewModel viewModel;
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;
    private ActivityResultLauncher<String> audioPermissionLauncher;

    private View themeSwatch;
    private TextView themeValue;
    private TextView ringtoneValue;
    private TextView pomodoroSummary;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_settings, R.string.settings_title);

        viewModel = new ViewModelProvider(
                this,
                ((App) getApplication()).getContainer().getViewModelFactory()
        ).get(SettingsHubViewModel.class);

        bindViews();
        registerLaunchers();
        bindClicks();
        observeVm();

        if (getIntent().getBooleanExtra(EXTRA_ENABLE_APP_BLOCKING, false)) {
            getIntent().removeExtra(EXTRA_ENABLE_APP_BLOCKING);
            AppBlockingEnabler.tryEnable(this, blockingEnablerHost);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.onHostResumed();
    }

    private void bindViews() {
        themeSwatch = findViewById(R.id.theme_swatch);
        themeValue = findViewById(R.id.theme_value);
        ringtoneValue = findViewById(R.id.ringtone_value);
        pomodoroSummary = findViewById(R.id.pomodoro_summary);
    }

    private void registerLaunchers() {
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    viewModel.onAudioPermissionResult(granted);
                });

        ringtonePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = getRingtonePickedUri(result.getData());
                        if (uri != null) {
                            viewModel.dispatch(SettingsHubIntent.customRingtonePicked(uri.toString()));
                            return;
                        }
                    }
                    viewModel.dispatch(SettingsHubIntent.customRingtoneCancelled());
                });
    }

    private void bindClicks() {
        findViewById(R.id.row_account_profile).setOnClickListener(
                v -> viewModel.dispatch(SettingsHubIntent.openAccountProfile()));
        findViewById(R.id.row_account_security).setOnClickListener(
                v -> viewModel.dispatch(SettingsHubIntent.openAccountSecurity()));
        findViewById(R.id.row_theme).setOnClickListener(
                v -> viewModel.dispatch(SettingsHubIntent.openTheme()));
        findViewById(R.id.row_ringtone).setOnClickListener(
                v -> viewModel.dispatch(SettingsHubIntent.openRingtone()));
        findViewById(R.id.row_pomodoro).setOnClickListener(
                v -> viewModel.dispatch(SettingsHubIntent.openPomodoro()));
        findViewById(R.id.row_system_permissions).setOnClickListener(
                v -> viewModel.dispatch(SettingsHubIntent.openSystemPermissions()));
    }

    private void observeVm() {
        viewModel.getUiState().observe(this, this::render);
        viewModel.getEffects().observe(this, this::handleEffect);
    }

    private void render(@Nullable SettingsHubUiState state) {
        if (state == null || isFinishing() || isDestroyed()) {
            return;
        }
        themeValue.setText(state.themeName);
        applyThemeSwatch(state.themeResId, state.themeGradient);
        ringtoneValue.setText(state.ringtoneLabel);
        pomodoroSummary.setText(state.pomodoroSummary);
    }

    private void applyThemeSwatch(int resId, boolean gradient) {
        try {
            if (gradient) {
                themeSwatch.setBackground(ContextCompat.getDrawable(this, resId));
                themeSwatch.setClipToOutline(true);
                themeSwatch.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
            } else {
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(ContextCompat.getColor(this, resId));
                themeSwatch.setBackground(shape);
            }
        } catch (Exception e) {
            themeSwatch.setBackgroundResource(R.drawable.bg_theme_swatch);
        }
    }

    private void handleEffect(@Nullable SettingsHubEffect effect) {
        if (effect == null || isFinishing() || isDestroyed()) {
            return;
        }
        switch (effect.type) {
            case SHOW_TOAST:
                Toast.makeText(this, effect.toastRes,
                        effect.toastLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
                break;
            case START_ACTIVITY:
                if (effect.activityClass != null) {
                    startActivity(new Intent(this, effect.activityClass));
                }
                break;
            case SHOW_RINGTONE_CHOOSER:
                showRingtoneChooser(effect.ringtoneSelectedIndex);
                break;
            case LAUNCH_RINGTONE_PICKER:
                launchRingtonePicker(effect.existingRingtoneUri);
                break;
            case REQUEST_AUDIO_PERMISSION:
                requestAudioPermission();
                break;
            default:
                break;
        }
    }

    private void showRingtoneChooser(int selectedIndex) {
        String[] options = getResources().getStringArray(R.array.ringtones);
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_ringtone)
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    viewModel.dispatch(SettingsHubIntent.ringtoneOptionSelected(which));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void requestAudioPermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onAudioPermissionResult(true);
            return;
        }
        if (shouldShowRequestPermissionRationale(permission)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.common_dialog_permission_title)
                    .setMessage(R.string.settings_dialog_storage_permission_message)
                    .setPositiveButton(R.string.confirm,
                            (d, w) -> audioPermissionLauncher.launch(permission))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            audioPermissionLauncher.launch(permission);
        }
    }

    private void launchRingtonePicker(@Nullable String existingUri) {
        try {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_ringtone));
            if (existingUri != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUri));
            }
            ringtonePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.settings_toast_ringtone_picker_failed, Toast.LENGTH_SHORT).show();
            viewModel.dispatch(SettingsHubIntent.customRingtoneCancelled());
        }
    }

    @Nullable
    private Uri getRingtonePickedUri(Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
        }
        return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
    }

    private final AppBlockingEnabler.Host blockingEnablerHost = new AppBlockingEnabler.Host() {
        @Override
        public Activity getActivity() {
            return SettingsActivity.this;
        }

        @Override
        public void onBlockingEnabled() {
            Toast.makeText(SettingsActivity.this, R.string.blocking_toast_enabled, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBlockingEnableFailed() {
            Toast.makeText(SettingsActivity.this, R.string.blocking_toast_permission_failed, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppBlockingEnabler.REQUEST_USAGE_STATS
                || requestCode == AppBlockingEnabler.REQUEST_OVERLAY_PERMISSION
                || requestCode == AppBlockingEnabler.REQUEST_QUERY_ALL_PACKAGES) {
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> AppBlockingEnabler.onPermissionActivityResult(
                            SettingsActivity.this, blockingEnablerHost, requestCode),
                    1000L);
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
