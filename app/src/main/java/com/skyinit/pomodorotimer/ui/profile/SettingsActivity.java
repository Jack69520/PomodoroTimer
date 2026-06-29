package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.AppBlockingEnabler;
import com.skyinit.pomodorotimer.util.ExactAlarmPermissionHelper;
import com.skyinit.pomodorotimer.util.FocusDndHelper;
import com.skyinit.pomodorotimer.util.StudyDurationPickerHelper;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends BaseActivity {
    public static final String EXTRA_ENABLE_APP_BLOCKING = "extra_enable_app_blocking";
    private static final int AUDIO_PERMISSION_CODE = 200;
    
    private SettingsManager settings;
    private Spinner themeSpinner;
    private Spinner ringtoneSpinner;
    private Spinner pauseCountSpinner;
    private Spinner breakDurationSpinner;
    private TextView studyDurationValue;
    private TextView studyDurationChangeButton;
    private Switch autoDeleteSwitch;
    private Switch dndDuringFocusSwitch;
    private Switch autoStartAfterBreakSwitch;
    private Switch longBreakEnabledSwitch;
    private Spinner pomodorosBeforeLongBreakSpinner;
    private Spinner longBreakDurationSpinner;
    private View longBreakIntervalRow;
    private View longBreakDurationRow;
    private SettingsViewModel settingsViewModel;
    private TextView exactAlarmStatusText;
    private TextView exactAlarmOpenButton;
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;
    
    private boolean isSpinnerInitialized = false;
    private boolean pendingDndPermissionRequest = false;
    private boolean suppressDndSwitchCallback = false;
    private int lastSelectedRingtonePosition = 0;
    private List<Integer> allThemeResources;
    
    // 标准色主题资源ID列表
    private static final List<Integer> STANDARD_THEME_COLORS = Arrays.asList(
            R.color.default_theme,
            R.color.theme1, R.color.theme2, R.color.theme3,
            R.color.theme4, R.color.theme5, R.color.theme6,
            R.color.theme7, R.color.theme8, R.color.theme9,
            R.color.theme10
    );

    // 中国色主题资源ID列表
    private static final List<Integer> CHINESE_THEME_COLORS = Arrays.asList(
            R.color.theme11, R.color.theme12, R.color.theme13,
            R.color.theme14, R.color.theme15, R.color.theme16,
            R.color.theme17, R.color.theme18, R.color.theme19,
            R.color.theme20
    );

    // 国风渐变色主题资源ID列表
    private static final List<Integer> GRADIENT_THEMES = Arrays.asList(
            R.drawable.themecolor21, R.drawable.themecolor22, R.drawable.themecolor23,
            R.drawable.themecolor24, R.drawable.themecolor25, R.drawable.themecolor26,
            R.drawable.themecolor27, R.drawable.themecolor28, R.drawable.themecolor29,
            R.drawable.themecolor30
    );

    // 莫兰迪色系主题资源ID列表
    private static final List<Integer> MORANDI_THEME_COLORS = Arrays.asList(
            R.color.morandi1, R.color.morandi2, R.color.morandi3,
            R.color.morandi4, R.color.morandi5, R.color.morandi6,
            R.color.morandi7, R.color.morandi8, R.color.morandi9,
            R.color.morandi10, R.color.morandi11, R.color.morandi12,
            R.color.morandi13, R.color.morandi14, R.color.morandi15
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
        
        settings = new SettingsManager(this);
        settingsViewModel = new ViewModelProvider(
                this,
                ((App) getApplication()).getContainer().getViewModelFactory()
        ).get(SettingsViewModel.class);
        
        initViews();
        setupRingtonePicker();
        setupUI();

        if (getIntent().getBooleanExtra(EXTRA_ENABLE_APP_BLOCKING, false)) {
            getIntent().removeExtra(EXTRA_ENABLE_APP_BLOCKING);
            AppBlockingEnabler.tryEnable(this, blockingEnablerHost);
        }
    }
    
    private void initViews() {
        themeSpinner = findViewById(R.id.theme_spinner);
        ringtoneSpinner = findViewById(R.id.ringtone_spinner);
        pauseCountSpinner = findViewById(R.id.pause_count_spinner);
        breakDurationSpinner = findViewById(R.id.break_duration_spinner);
        studyDurationValue = findViewById(R.id.study_duration_value);
        studyDurationChangeButton = findViewById(R.id.study_duration_change_button);
        autoDeleteSwitch = findViewById(R.id.auto_delete_switch);
        dndDuringFocusSwitch = findViewById(R.id.dnd_during_focus_switch);
        autoStartAfterBreakSwitch = findViewById(R.id.auto_start_after_break_switch);
        longBreakEnabledSwitch = findViewById(R.id.long_break_enabled_switch);
        pomodorosBeforeLongBreakSpinner = findViewById(R.id.pomodoros_before_long_break_spinner);
        longBreakDurationSpinner = findViewById(R.id.long_break_duration_spinner);
        longBreakIntervalRow = findViewById(R.id.long_break_interval_row);
        longBreakDurationRow = findViewById(R.id.long_break_duration_row);
        exactAlarmStatusText = findViewById(R.id.exact_alarm_status_text);
        exactAlarmOpenButton = findViewById(R.id.exact_alarm_open_button);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshExactAlarmPermissionUi();
        refreshDndPermissionUi();
        refreshStudyDurationUi();
    }

    /** 刷新精确闹钟权限状态（用户可能在系统设置中撤回授权）。 */
    private void refreshExactAlarmPermissionUi() {
        if (exactAlarmStatusText == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            exactAlarmStatusText.setText(R.string.exact_alarm_status_not_required);
            if (exactAlarmOpenButton != null) {
                exactAlarmOpenButton.setVisibility(View.GONE);
            }
            return;
        }
        boolean granted = ExactAlarmPermissionHelper.canScheduleExactAlarms(this);
        exactAlarmStatusText.setText(granted
                ? R.string.exact_alarm_status_granted
                : R.string.exact_alarm_status_denied);
        if (exactAlarmOpenButton != null) {
            exactAlarmOpenButton.setVisibility(granted ? View.GONE : View.VISIBLE);
            exactAlarmOpenButton.setOnClickListener(v ->
                    ExactAlarmPermissionHelper.openExactAlarmSettings(SettingsActivity.this));
        }
    }

    /** 同步勿扰开关与系统权限：未授权时保持关闭并回写用户设置。 */
    private void refreshDndPermissionUi() {
        if (dndDuringFocusSwitch == null) {
            return;
        }
        boolean hasAccess = FocusDndHelper.hasPolicyAccess(this);

        if (pendingDndPermissionRequest) {
            pendingDndPermissionRequest = false;
            if (hasAccess) {
                settingsViewModel.setDndDuringFocusEnabled(true);
                setDndSwitchChecked(true);
            } else {
                settingsViewModel.setDndDuringFocusEnabled(false);
                setDndSwitchChecked(false);
            }
            return;
        }

        UserPomodoroSettings settings = settingsViewModel.getPomodoroSettings().getValue();
        boolean savedEnabled = settings != null && settings.dndDuringFocusEnabled;
        if (!hasAccess) {
            if (savedEnabled) {
                settingsViewModel.setDndDuringFocusEnabled(false);
            }
            setDndSwitchChecked(false);
        } else {
            setDndSwitchChecked(savedEnabled);
        }
    }

    private void setDndSwitchChecked(boolean checked) {
        if (dndDuringFocusSwitch == null) {
            return;
        }
        suppressDndSwitchCallback = true;
        dndDuringFocusSwitch.setChecked(checked);
        suppressDndSwitchCallback = false;
    }
    
    private void setupRingtonePicker() {
        ringtonePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = getRingtonePickedUri(result.getData());
                        if (uri != null) {
                            onRingtoneSelected(uri);
                        } else {
                            ringtoneSpinner.setSelection(lastSelectedRingtonePosition);
                        }
                    } else {
                        ringtoneSpinner.setSelection(lastSelectedRingtonePosition);
                    }
                }
        );
    }
    
    private void setupUI() {
        // 设置主题选择器
        setupCategorizedThemeSpinner();
        
        // 设置铃声选择器
        setupRingtoneSpinner();
        
        // 设置番茄钟设置
        setupPomodoroSettings();
        setupPomodoroWorkflowSettings();
        
        isSpinnerInitialized = true;
    }
    
    private void setupCategorizedThemeSpinner() {
        // 创建分类主题列表
        List<String> allThemeNames = new ArrayList<>();
        List<Integer> allThemeResources = new ArrayList<>();
        
        // 添加标准色主题
        String[] standardNames = getResources().getStringArray(R.array.standard_theme_names);
        for (int i = 0; i < standardNames.length; i++) {
            allThemeNames.add(getString(R.string.settings_theme_prefix_standard, standardNames[i]));
            allThemeResources.add(STANDARD_THEME_COLORS.get(i));
        }
        
        // 添加中国色主题
        String[] chineseNames = getResources().getStringArray(R.array.chinese_theme_names);
        for (int i = 0; i < chineseNames.length; i++) {
            allThemeNames.add(getString(R.string.settings_theme_prefix_chinese, chineseNames[i]));
            allThemeResources.add(CHINESE_THEME_COLORS.get(i));
        }
        
        // 添加国风渐变色主题
        String[] gradientNames = getResources().getStringArray(R.array.gradient_theme_names);
        for (int i = 0; i < gradientNames.length; i++) {
            allThemeNames.add(getString(R.string.settings_theme_prefix_gradient, gradientNames[i]));
            allThemeResources.add(GRADIENT_THEMES.get(i));
        }
        
        // 添加莫兰迪色系主题
        String[] morandiNames = getResources().getStringArray(R.array.morandi_theme_names);
        for (int i = 0; i < morandiNames.length; i++) {
            allThemeNames.add(getString(R.string.settings_theme_prefix_morandi, morandiNames[i]));
            allThemeResources.add(MORANDI_THEME_COLORS.get(i));
        }
        
        // 创建适配器
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, allThemeNames);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(themeAdapter);
        
        // 保存所有主题资源列表供后续使用
        this.allThemeResources = allThemeResources;
        
        // 初始化选择位置
        int currentThemeColor = settings.getThemeColor();
        int currentThemeIndex = allThemeResources.indexOf(currentThemeColor);
        if (currentThemeIndex >= 0) {
            themeSpinner.setSelection(currentThemeIndex);
        } else {
            themeSpinner.setSelection(0);
        }
        
        // 主题选择器监听器
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerInitialized) return;
                
                // 获取选中的主题颜色资源
                int selectedResId = allThemeResources.get(position);
                
                // 如果用户选择默认主题
                if (selectedResId == R.color.default_theme ||
                        selectedResId == R.color.default_theme_dark) {
                    // 清除深色模式覆盖标记
                    settings.setThemeColor(selectedResId);
                }
                // 如果用户选择非默认主题
                else {
                    // 设置主题并标记为覆盖
                    settings.setThemeColor(selectedResId);
                    settings.setDarkModeOverride();
                }
                
                // 应用主题并通知
                applyTheme();
                SettingsActivity.this.currentThemeColor = selectedResId;
                applyThemeWithNotification();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void setupRingtoneSpinner() {
        ArrayAdapter<CharSequence> ringtoneAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.ringtones,
                android.R.layout.simple_spinner_item
        );
        ringtoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ringtoneSpinner.setAdapter(ringtoneAdapter);

        applyRingtoneSpinnerSelection(settings.getRingtoneUri());
        ringtoneSpinner.setOnItemSelectedListener(createRingtoneItemSelectedListener());
    }
    
    private void setupPomodoroSettings() {
        setupStudyDurationSetting();
        setupBreakDurationSpinner();

        // 设置暂停次数选择器
        String[] pauseCountOptions = getResources().getStringArray(R.array.settings_pause_count_options);
        ArrayAdapter<String> pauseCountAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, pauseCountOptions);
        pauseCountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pauseCountSpinner.setAdapter(pauseCountAdapter);

        pauseCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerInitialized) {
                    return;
                }
                settingsViewModel.setMaxPauseCount(position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (dndDuringFocusSwitch != null) {
            dndDuringFocusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSpinnerInitialized || suppressDndSwitchCallback) {
                    return;
                }
                if (isChecked && !FocusDndHelper.hasPolicyAccess(this)) {
                    setDndSwitchChecked(false);
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.settings_dnd_permission_title)
                            .setMessage(R.string.settings_dnd_permission_message)
                            .setPositiveButton(R.string.confirm, (d, w) -> {
                                pendingDndPermissionRequest = true;
                                startActivity(FocusDndHelper.createPolicyAccessIntent(this));
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                    return;
                }
                settingsViewModel.setDndDuringFocusEnabled(isChecked);
            });
        }
        
        // 设置自动删除开关
        autoDeleteSwitch.setChecked(settings.isAutoDeleteEnabled());
        autoDeleteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setAutoDeleteEnabled(isChecked);
        });
    }

    private void setupPomodoroWorkflowSettings() {
        setupLongBreakSpinners();

        settingsViewModel.getPomodoroSettings().observe(this, this::applyPomodoroSettingsToUi);

        if (autoStartAfterBreakSwitch != null) {
            autoStartAfterBreakSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSpinnerInitialized) {
                    return;
                }
                settingsViewModel.setAutoStartAfterBreak(isChecked);
            });
        }

        if (longBreakEnabledSwitch != null) {
            longBreakEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isSpinnerInitialized) {
                    return;
                }
                settingsViewModel.setLongBreakEnabled(isChecked);
                updateLongBreakControlsVisibility(isChecked);
            });
        }
    }

    private void setupLongBreakSpinners() {
        if (pomodorosBeforeLongBreakSpinner != null) {
            String[] intervalOptions = getResources().getStringArray(R.array.pomodoros_before_long_break_options);
            ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, intervalOptions);
            intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            pomodorosBeforeLongBreakSpinner.setAdapter(intervalAdapter);
            pomodorosBeforeLongBreakSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!isSpinnerInitialized) {
                        return;
                    }
                    settingsViewModel.setPomodorosBeforeLongBreak(position + UserPomodoroSettings.MIN_POMODOROS_BEFORE_LONG_BREAK);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        if (longBreakDurationSpinner != null) {
            String[] durationOptions = getResources().getStringArray(R.array.long_break_duration_options);
            ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, durationOptions);
            durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            longBreakDurationSpinner.setAdapter(durationAdapter);
            longBreakDurationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!isSpinnerInitialized) {
                        return;
                    }
                    settingsViewModel.setLongBreakDurationMinutes(10 + position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void applyPomodoroSettingsToUi(UserPomodoroSettings pomodoroSettings) {
        if (pomodoroSettings == null) {
            return;
        }

        refreshStudyDurationUi(pomodoroSettings.defaultStudyTimeMs);
        applyBreakDurationSelection(pomodoroSettings.defaultBreakTimeMs);

        if (pauseCountSpinner != null) {
            int pauseIndex = Math.max(0, Math.min(pomodoroSettings.maxPauseCount - 1,
                    getResources().getStringArray(R.array.settings_pause_count_options).length - 1));
            pauseCountSpinner.setSelection(pauseIndex, false);
        }
        if (dndDuringFocusSwitch != null) {
            boolean hasAccess = FocusDndHelper.hasPolicyAccess(this);
            setDndSwitchChecked(pomodoroSettings.dndDuringFocusEnabled && hasAccess);
        }

        if (autoStartAfterBreakSwitch != null) {
            autoStartAfterBreakSwitch.setChecked(pomodoroSettings.autoStartAfterBreak);
        }
        if (longBreakEnabledSwitch != null) {
            longBreakEnabledSwitch.setChecked(pomodoroSettings.longBreakEnabled);
            updateLongBreakControlsVisibility(pomodoroSettings.longBreakEnabled);
        }
        if (pomodorosBeforeLongBreakSpinner != null) {
            int index = Math.max(0, pomodoroSettings.pomodorosBeforeLongBreak
                    - UserPomodoroSettings.MIN_POMODOROS_BEFORE_LONG_BREAK);
            pomodorosBeforeLongBreakSpinner.setSelection(index, false);
        }
        if (longBreakDurationSpinner != null) {
            long minutes = pomodoroSettings.longBreakDurationMs / 60_000L;
            int index = (int) Math.max(0, Math.min(5, minutes - 10));
            longBreakDurationSpinner.setSelection(index, false);
        }
    }

    private void updateLongBreakControlsVisibility(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        if (longBreakIntervalRow != null) {
            longBreakIntervalRow.setVisibility(visibility);
        }
        if (longBreakDurationRow != null) {
            longBreakDurationRow.setVisibility(visibility);
        }
    }

    private void setupStudyDurationSetting() {
        refreshStudyDurationUi(((App) getApplication()).getContainer()
                .getTimerSettingsRepository().getDefaultStudyTimeMs());
        if (studyDurationChangeButton != null) {
            studyDurationChangeButton.setOnClickListener(v -> StudyDurationPickerHelper.show(
                    this,
                    getString(R.string.settings_study_duration),
                    getString(R.string.confirm),
                    ((App) getApplication()).getContainer().getTimerSettingsRepository().getDefaultStudyTimeMs(),
                    (totalMinutes, millis) -> {
                        settingsViewModel.setDefaultStudyTimeMs(millis);
                        Toast.makeText(this, R.string.study_duration_updated, Toast.LENGTH_SHORT).show();
                    }));
        }
    }

    private void refreshStudyDurationUi(long studyTimeMs) {
        if (studyDurationValue == null) {
            return;
        }
        studyDurationValue.setText(getString(R.string.study_duration_current,
                StudyDurationPickerHelper.formatDurationLabel(this, studyTimeMs)));
    }

    private void refreshStudyDurationUi() {
        refreshStudyDurationUi(((App) getApplication()).getContainer()
                .getTimerSettingsRepository().getDefaultStudyTimeMs());
    }

    private void applyBreakDurationSelection(long breakTimeMs) {
        if (breakDurationSpinner == null) {
            return;
        }
        int currentMinutes = (int) (breakTimeMs / 60_000L);
        String[] breakOptions = getResources().getStringArray(R.array.break_duration_options);
        int selectedIndex = Math.max(0, Math.min(currentMinutes - 1, breakOptions.length - 1));
        breakDurationSpinner.setSelection(selectedIndex, false);
    }

    private void setupBreakDurationSpinner() {
        if (breakDurationSpinner == null) {
            return;
        }
        String[] breakOptions = getResources().getStringArray(R.array.break_duration_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, breakOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        breakDurationSpinner.setAdapter(adapter);

        breakDurationSpinner.setAdapter(adapter);

        AdapterView.OnItemSelectedListener breakListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerInitialized) {
                    return;
                }
                long minutes = position + 1L;
                settingsViewModel.setDefaultBreakTimeMs(minutes * 60_000L);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        breakDurationSpinner.setOnItemSelectedListener(breakListener);
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
                    () -> AppBlockingEnabler.onPermissionActivityResult(this, blockingEnablerHost),
                    1000L);
        }
    }
    
    private void applyThemeWithNotification() {
        try {
            // 使用单次通知
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) {
                    Toast.makeText(this, R.string.settings_toast_theme_applied, Toast.LENGTH_SHORT).show();
                }
            }, 300);
        } catch (Exception e) {
            // 主题应用失败处理
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) {
                    Toast.makeText(this, R.string.settings_toast_theme_failed, Toast.LENGTH_SHORT).show();
                }
            }, 300);
        }
    }
    
    private void setRingtoneSelectionWithoutTrigger(String uri) {
        applyRingtoneSpinnerSelection(uri);
        ringtoneSpinner.setOnItemSelectedListener(createRingtoneItemSelectedListener());
    }

    private void applyRingtoneSpinnerSelection(String uri) {
        if (ringtoneSpinner == null) return;
        int position = getRingtoneSpinnerPositionForUri(uri);
        ringtoneSpinner.setOnItemSelectedListener(null);
        ringtoneSpinner.setSelection(position, false);
        lastSelectedRingtonePosition = position;
    }

    private int getRingtoneSpinnerPositionForUri(String uri) {
        if (uri == null || "default".equals(uri) ||
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString().equals(uri)) {
            return 0;
        }
        if ("silent".equals(uri)) {
            return 1;
        }
        return 2;
    }

    private AdapterView.OnItemSelectedListener createRingtoneItemSelectedListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleRingtoneSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    private void handleRingtoneSelection(int position) {
        switch (position) {
            case 0: // 系统默认
                lastSelectedRingtonePosition = position;
                settings.setRingtoneUri("default");
                break;
            case 1: // 静音
                lastSelectedRingtonePosition = position;
                settings.setRingtoneUri("silent");
                break;
            case 2: // 自定义铃声
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isFinishing()) {
                        pickCustomRingtone();
                    }
                }, 300);
                break;
        }
    }

    @Nullable
    private Uri getRingtonePickedUri(Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
        }
        return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
    }
    
    private void pickCustomRingtone() {
        if (isFinishing()) return;
        launchRingtonePicker();
    }

    private boolean checkAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 9 及以下不需要权限
    }

    private void requestAudioPermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        String[] permissions = {permission};

        // 检查是否已经拒绝过权限
        if (shouldShowRequestPermissionRationale(permission)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.common_dialog_permission_title)
                    .setMessage(R.string.settings_dialog_storage_permission_message)
                    .setPositiveButton(R.string.confirm, (d, w) -> requestPermissions(permissions, AUDIO_PERMISSION_CODE))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            requestPermissions(permissions, AUDIO_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchRingtonePicker();
            } else {
                Toast.makeText(this, R.string.settings_toast_storage_permission_denied, Toast.LENGTH_SHORT).show();
                ringtoneSpinner.setSelection(lastSelectedRingtonePosition);
            }
        }
    }

    private void launchRingtonePicker() {
        try {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_ringtone));

            String currentUri = settings.getRingtoneUri();
            if (!"default".equals(currentUri) && !"silent".equals(currentUri)) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentUri));
            }

            ringtonePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.settings_toast_ringtone_picker_failed, Toast.LENGTH_SHORT).show();
            ringtoneSpinner.setSelection(lastSelectedRingtonePosition);
        }
    }

    private void onRingtoneSelected(Uri uri) {
        if (uri != null) {
            settings.setRingtoneUri(uri.toString());
            lastSelectedRingtonePosition = 2;
            setRingtoneSelectionWithoutTrigger(uri.toString());
            Toast.makeText(this, R.string.settings_toast_custom_ringtone_success, Toast.LENGTH_SHORT).show();
        } else {
            ringtoneSpinner.setSelection(lastSelectedRingtonePosition);
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
        // 添加简单的退出动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
