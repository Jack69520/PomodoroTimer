package com.skyinit.pomodorotimer.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;
import com.skyinit.pomodorotimer.ui.profile.devlab.DevLabEffect;
import com.skyinit.pomodorotimer.ui.profile.devlab.DevLabIntent;
import com.skyinit.pomodorotimer.ui.profile.devlab.DevLabLogAdapter;
import com.skyinit.pomodorotimer.ui.profile.devlab.DevLabUiState;
import com.skyinit.pomodorotimer.ui.profile.devlab.DevLabViewModel;

/**
 * 开发实验室：薄 UI 层，仅观察 {@link DevLabViewModel} 状态与副作用。
 */
public class DevLabActivity extends SubpageActivity {

    private DevLabViewModel viewModel;
    private DevLabLogAdapter logAdapter;

    private TextView tvAppInfoError;
    private View layoutAppInfoRows;
    private TextView tvAppName;
    private TextView tvVersionName;
    private TextView tvVersionCode;

    private TextView tvDeviceName;
    private TextView tvDeviceModel;
    private TextView tvDeviceBrand;
    private TextView tvLanguage;
    private TextView tvAndroidVersion;
    private TextView tvApiLevel;

    private MaterialButton btnRefreshResources;
    private SwitchCompat switchAutoUpdate;
    private ProgressBar progressStorage;
    private TextView tvStorageSummary;
    private ProgressBar progressStorageLoading;
    private ProgressBar progressMemory;
    private TextView tvMemorySummary;
    private ProgressBar progressMemoryLoading;

    private MaterialButton btnRefreshLogs;
    private MaterialButton btnClearLogs;
    private TextView tvLogCount;
    private Spinner spLogFilter;
    private RecyclerView rvLogs;
    private TextView tvLogsEmpty;
    private ProgressBar progressLogsLoading;

    private boolean suppressAutoUpdateCallback;
    private boolean suppressFilterCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_dev_lab, R.string.title_dev_lab);

        viewModel = new ViewModelProvider(
                this,
                ((App) getApplication()).getContainer().getViewModelFactory()
        ).get(DevLabViewModel.class);

        bindViews();
        setupLogsList();
        setupFilter();
        setupActions();

        viewModel.getUiState().observe(this, this::render);
        viewModel.getEffects().observe(this, this::handleEffect);
        viewModel.dispatch(DevLabIntent.bootstrap());
    }

    private void bindViews() {
        tvAppInfoError = findViewById(R.id.tv_app_info_error);
        layoutAppInfoRows = findViewById(R.id.layout_app_info_rows);
        tvAppName = findViewById(R.id.tv_app_name);
        tvVersionName = findViewById(R.id.tv_version_name);
        tvVersionCode = findViewById(R.id.tv_version_code);

        tvDeviceName = findViewById(R.id.tv_device_name);
        tvDeviceModel = findViewById(R.id.tv_device_model);
        tvDeviceBrand = findViewById(R.id.tv_device_brand);
        tvLanguage = findViewById(R.id.tv_language);
        tvAndroidVersion = findViewById(R.id.tv_android_version);
        tvApiLevel = findViewById(R.id.tv_api_level);

        btnRefreshResources = findViewById(R.id.btn_refresh_resources);
        switchAutoUpdate = findViewById(R.id.switch_auto_update);
        progressStorage = findViewById(R.id.progress_storage);
        tvStorageSummary = findViewById(R.id.tv_storage_summary);
        progressStorageLoading = findViewById(R.id.progress_storage_loading);
        progressMemory = findViewById(R.id.progress_memory);
        tvMemorySummary = findViewById(R.id.tv_memory_summary);
        progressMemoryLoading = findViewById(R.id.progress_memory_loading);

        btnRefreshLogs = findViewById(R.id.btn_refresh_logs);
        btnClearLogs = findViewById(R.id.btn_clear_logs);
        tvLogCount = findViewById(R.id.tv_log_count);
        spLogFilter = findViewById(R.id.sp_log_filter);
        rvLogs = findViewById(R.id.rv_logs);
        tvLogsEmpty = findViewById(R.id.tv_logs_empty);
        progressLogsLoading = findViewById(R.id.progress_logs_loading);
    }

    private void setupLogsList() {
        logAdapter = new DevLabLogAdapter();
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setItemAnimator(null);
        rvLogs.setAdapter(logAdapter);
        rvLogs.setNestedScrollingEnabled(true);
    }

    private void setupFilter() {
        String[] options = getResources().getStringArray(R.array.dev_lab_log_levels);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLogFilter.setAdapter(adapter);
        spLogFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressFilterCallback) {
                    return;
                }
                viewModel.dispatch(DevLabIntent.setFilterIndex(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupActions() {
        btnRefreshResources.setOnClickListener(v ->
                viewModel.dispatch(DevLabIntent.refreshResources()));
        btnRefreshLogs.setOnClickListener(v ->
                viewModel.dispatch(DevLabIntent.refreshLogs()));
        btnClearLogs.setOnClickListener(v ->
                viewModel.dispatch(DevLabIntent.requestClearLogs()));

        switchAutoUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressAutoUpdateCallback) {
                return;
            }
            viewModel.dispatch(DevLabIntent.setAutoUpdate(isChecked));
        });
    }

    private void render(@Nullable DevLabUiState state) {
        if (state == null || isFinishing() || isDestroyed()) {
            return;
        }

        tvAppInfoError.setVisibility(state.appInfoError ? View.VISIBLE : View.GONE);
        layoutAppInfoRows.setVisibility(state.appInfoError ? View.GONE : View.VISIBLE);
        tvAppName.setText(state.appName);
        tvVersionName.setText(state.versionName);
        tvVersionCode.setText(String.valueOf(state.versionCode));

        tvDeviceName.setText(state.deviceName);
        tvDeviceModel.setText(state.deviceModel);
        tvDeviceBrand.setText(state.deviceBrand);
        tvLanguage.setText(state.language);
        tvAndroidVersion.setText(state.androidVersion);
        tvApiLevel.setText(state.apiLevel);

        if (switchAutoUpdate.isChecked() != state.autoUpdateEnabled) {
            suppressAutoUpdateCallback = true;
            switchAutoUpdate.setChecked(state.autoUpdateEnabled);
            suppressAutoUpdateCallback = false;
        }

        boolean resourcesBusy = state.storageLoading || state.memoryLoading;
        btnRefreshResources.setEnabled(!resourcesBusy);
        progressStorageLoading.setVisibility(state.storageLoading ? View.VISIBLE : View.GONE);
        progressMemoryLoading.setVisibility(state.memoryLoading ? View.VISIBLE : View.GONE);

        progressStorage.setProgress(state.storagePercent);
        if (state.storageError) {
            tvStorageSummary.setText(R.string.dev_lab_error_storage_info);
        } else {
            tvStorageSummary.setText(getString(
                    R.string.dev_lab_runtime_summary,
                    state.appStorage,
                    state.usedStorage,
                    state.totalStorage,
                    state.storagePercentText
            ));
        }

        progressMemory.setProgress(state.memoryPercent);
        if (state.memoryError) {
            tvMemorySummary.setText(R.string.dev_lab_error_memory_info);
        } else {
            tvMemorySummary.setText(getString(
                    R.string.dev_lab_runtime_summary,
                    state.appMemory,
                    state.usedMemory,
                    state.totalMemory,
                    state.memoryPercentText
            ));
        }

        btnRefreshLogs.setEnabled(!state.logsLoading);
        btnClearLogs.setEnabled(!state.logsLoading && state.totalLogCount > 0);
        progressLogsLoading.setVisibility(state.logsLoading ? View.VISIBLE : View.GONE);

        if (spLogFilter.getSelectedItemPosition() != state.filterIndex) {
            suppressFilterCallback = true;
            spLogFilter.setSelection(state.filterIndex, false);
            suppressFilterCallback = false;
        }

        tvLogCount.setText(getString(R.string.dev_lab_log_count_format,
                state.visibleLogs.size(), state.totalLogCount));
        logAdapter.submitList(state.visibleLogs);
        boolean empty = !state.logsLoading && state.visibleLogs.isEmpty();
        tvLogsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvLogs.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);
    }

    private void handleEffect(@Nullable DevLabEffect effect) {
        if (effect == null || isFinishing() || isDestroyed()) {
            return;
        }
        switch (effect.type) {
            case TOAST:
                Toast.makeText(this, effect.messageRes, Toast.LENGTH_SHORT).show();
                break;
            case SHOW_CLEAR_CONFIRM:
                ModernPromptDialog.builder(this)
                        .icon(R.drawable.ic_info)
                        .accent(ModernPromptDialog.Accent.DANGER)
                        .title(R.string.dev_lab_clear_confirm_title)
                        .message(R.string.dev_lab_clear_confirm_message)
                        .primaryDanger(R.string.dev_lab_btn_clear,
                                () -> viewModel.dispatch(DevLabIntent.confirmClearLogs()))
                        .tertiary(R.string.cancel, null)
                        .show();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (viewModel != null) {
            viewModel.dispatch(DevLabIntent.setPageVisible(true));
        }
    }

    @Override
    protected void onStop() {
        if (viewModel != null) {
            viewModel.dispatch(DevLabIntent.setPageVisible(false));
        }
        super.onStop();
    }
}
