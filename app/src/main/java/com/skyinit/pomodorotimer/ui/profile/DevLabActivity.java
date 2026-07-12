package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.util.SystemInfoUtils;
import com.skyinit.pomodorotimer.R;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.skyinit.pomodorotimer.util.AppLog;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DevLabActivity extends BaseActivity {
    private static final String TAG = "DevLabActivity";
    
    private TextView tvAppInfo;
    private TextView tvDeviceInfo;
    private TextView tvStorageInfo;
    private TextView tvMemoryInfo;
    private RecyclerView rvLogs;
    private Spinner spLogFilter;
    private Button btnRefreshLogs;
    private Button btnClearLogs;
    private Button btnRefreshStorage;
    private Button btnRefreshMemory;
    private Switch switchAutoUpdate;
    
    private LogAdapter logAdapter;
    private List<LogEntry> logEntries = new ArrayList<>();
    private String currentFilter;
    
    private static final int LOG_LEVEL_INFO = 0;
    private static final int LOG_LEVEL_ERROR = 1;
    private static final int LOG_LEVEL_WARN = 2;
    private static final int LOG_LEVEL_DEBUG = 3;
    
    // 动态更新相关
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private static final long UPDATE_INTERVAL = 5000; // 5秒更新一次
    private boolean isAutoUpdateEnabled = false;

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_dev_lab));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_lab);
        setupToolbar();
        
        initViews();
        setupLogFilter();
        loadAppInfo();
        loadDeviceInfo();
        loadStorageInfo();
        loadMemoryInfo();
        loadLogs();
    }
    
    private void initViews() {
        tvAppInfo = findViewById(R.id.tv_app_info);
        tvDeviceInfo = findViewById(R.id.tv_device_info);
        tvStorageInfo = findViewById(R.id.tv_storage_info);
        tvMemoryInfo = findViewById(R.id.tv_memory_info);
        rvLogs = findViewById(R.id.rv_logs);
        spLogFilter = findViewById(R.id.sp_log_filter);
        btnRefreshLogs = findViewById(R.id.btn_refresh_logs);
        btnClearLogs = findViewById(R.id.btn_clear_logs);
        btnRefreshStorage = findViewById(R.id.btn_refresh_storage);
        btnRefreshMemory = findViewById(R.id.btn_refresh_memory);
        switchAutoUpdate = findViewById(R.id.switch_auto_update);
        
        // 设置日志列表
        logAdapter = new LogAdapter(logEntries);
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setAdapter(logAdapter);
        
        // 设置按钮监听器
        btnRefreshLogs.setOnClickListener(v -> loadLogs());
        btnClearLogs.setOnClickListener(v -> clearLogs());
        btnRefreshStorage.setOnClickListener(v -> loadStorageInfo());
        btnRefreshMemory.setOnClickListener(v -> loadMemoryInfo());
        
        // 设置自动更新开关监听器
        switchAutoUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAutoUpdateEnabled = isChecked;
            if (isChecked) {
                startAutoUpdate();
                addLogEntry(logLevel(LOG_LEVEL_INFO),
                        getString(R.string.dev_lab_log_category_auto_update),
                        getString(R.string.dev_lab_log_auto_update_enabled));
            } else {
                stopAutoUpdate();
                addLogEntry(logLevel(LOG_LEVEL_INFO),
                        getString(R.string.dev_lab_log_category_auto_update),
                        getString(R.string.dev_lab_log_auto_update_disabled));
            }
            filterLogs();
        });
    }
    
    private void setupLogFilter() {
        String[] filterOptions = getResources().getStringArray(R.array.dev_lab_log_levels);
        currentFilter = filterOptions[0];
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, filterOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLogFilter.setAdapter(adapter);
        
        spLogFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentFilter = filterOptions[position];
                filterLogs();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void loadAppInfo() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String appName = getString(R.string.app_name);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            
            String appInfo = getString(
                R.string.dev_lab_format_app_info,
                appName, versionName, versionCode
            );
            
            tvAppInfo.setText(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            tvAppInfo.setText(R.string.dev_lab_error_app_info);
            AppLog.e(TAG, "Error getting app info", e);
        }
    }
    
    private void loadDeviceInfo() {
        String deviceName = Build.MODEL;
        String deviceModel = Build.MODEL;
        String deviceBrand = Build.BRAND;
        String language = Locale.getDefault().getDisplayLanguage();
        String androidVersion = Build.VERSION.RELEASE;
        String fullApiLevel = formatFullApiLevel();
        
        String deviceInfo = getString(
            R.string.dev_lab_format_device_info,
            deviceName, deviceModel, deviceBrand, language, androidVersion, fullApiLevel
        );
        
        tvDeviceInfo.setText(deviceInfo);
    }

    /** Android 16+ 使用 SDK_INT_FULL 区分 36.0 / 36.1 等小版本；更低版本回退为主版本 + .0 */
    private static String formatFullApiLevel() {
        if (Build.VERSION.SDK_INT >= 36) {
            int sdkFull = Build.VERSION.SDK_INT_FULL;
            int major = sdkFull / 100_000;
            int minor = sdkFull % 100_000;
            return String.format(Locale.US, "%d.%d", major, minor);
        }
        return String.format(Locale.US, "%d.0", Build.VERSION.SDK_INT);
    }
    
    private void loadStorageInfo() {
        try {
            String appStorage = SystemInfoUtils.getAppStorageSize(this);
            String totalStorage = SystemInfoUtils.getTotalStorageSize();
            String usedStorage = SystemInfoUtils.getUsedStorageSize();
            String storagePercentage = SystemInfoUtils.getStorageUsagePercentage();
            
            String storageInfo = getString(
                R.string.dev_lab_format_storage_info,
                appStorage, totalStorage, usedStorage, storagePercentage
            );
            
            tvStorageInfo.setText(storageInfo);
            
            addLogEntry(logLevel(LOG_LEVEL_INFO),
                    getString(R.string.dev_lab_log_category_storage),
                    getString(R.string.dev_lab_log_storage_updated));
        } catch (Exception e) {
            tvStorageInfo.setText(R.string.dev_lab_error_storage_info);
            addLogEntry(logLevel(LOG_LEVEL_ERROR),
                    getString(R.string.dev_lab_log_category_storage),
                    getString(R.string.dev_lab_log_storage_failed, e.getMessage()));
            AppLog.e(TAG, "Error loading storage info", e);
        }
    }
    
    private void loadMemoryInfo() {
        try {
            String appMemory = SystemInfoUtils.getAppMemoryUsage(this);
            String totalMemory = SystemInfoUtils.getTotalMemorySize();
            String usedMemory = SystemInfoUtils.getUsedMemorySize(this);
            String memoryPercentage = SystemInfoUtils.getMemoryUsagePercentage(this);
            
            String memoryInfo = getString(
                R.string.dev_lab_format_memory_info,
                appMemory, totalMemory, usedMemory, memoryPercentage
            );
            
            tvMemoryInfo.setText(memoryInfo);
            
            addLogEntry(logLevel(LOG_LEVEL_INFO),
                    getString(R.string.dev_lab_log_category_memory),
                    getString(R.string.dev_lab_log_memory_updated));
        } catch (Exception e) {
            tvMemoryInfo.setText(R.string.dev_lab_error_memory_info);
            addLogEntry(logLevel(LOG_LEVEL_ERROR),
                    getString(R.string.dev_lab_log_category_memory),
                    getString(R.string.dev_lab_log_memory_failed, e.getMessage()));
            AppLog.e(TAG, "Error loading memory info", e);
        }
    }
    
    private void loadLogs() {
        logEntries.clear();

        addLogEntry(logLevel(LOG_LEVEL_INFO),
                getString(R.string.dev_lab_log_category_app_start),
                getString(R.string.dev_lab_log_app_start));

        int logcatCount = 0;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("logcat", "-d", "-v", "time");
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String packageName = getPackageName();
            while ((line = reader.readLine()) != null && logcatCount < 50) {
                if (line.contains("PomodoroTimer") || line.contains(packageName)) {
                    String level = logLevel(LOG_LEVEL_INFO);
                    if (line.contains(" E/")) {
                        level = logLevel(LOG_LEVEL_ERROR);
                    } else if (line.contains(" W/")) {
                        level = logLevel(LOG_LEVEL_WARN);
                    } else if (line.contains(" D/")) {
                        level = logLevel(LOG_LEVEL_DEBUG);
                    } else if (line.contains(" I/")) {
                        level = logLevel(LOG_LEVEL_INFO);
                    }

                    addLogEntry(level,
                            getString(R.string.dev_lab_log_system),
                            line);
                    logcatCount++;
                }
            }
            reader.close();
        } catch (IOException e) {
            addLogEntry(logLevel(LOG_LEVEL_ERROR),
                    getString(R.string.dev_lab_log_category_log_read),
                    getString(R.string.dev_lab_log_read_failed, e.getMessage()));
        }

        if (logcatCount == 0 && logEntries.size() == 1) {
            addLogEntry(logLevel(LOG_LEVEL_INFO),
                    getString(R.string.dev_lab_log_category_log_read),
                    getString(R.string.dev_lab_log_empty));
        }

        filterLogs();
    }
    
    private void addLogEntry(String level, String tag, String message) {
        LogEntry entry = new LogEntry();
        entry.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        entry.level = level;
        entry.tag = tag;
        entry.message = message;
        logEntries.add(entry);
    }
    
    private void filterLogs() {
        List<LogEntry> filteredEntries = new ArrayList<>();
        
        for (LogEntry entry : logEntries) {
            if (getString(R.string.common_filter_all).equals(currentFilter)
                    || entry.level.equals(currentFilter)) {
                filteredEntries.add(entry);
            }
        }
        
        logAdapter.updateLogs(filteredEntries);
    }
    
    private void clearLogs() {
        logEntries.clear();
        logAdapter.updateLogs(logEntries);
        Toast.makeText(this, R.string.dev_lab_toast_logs_cleared, Toast.LENGTH_SHORT).show();
    }
    
    private void startAutoUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoUpdateEnabled && !isFinishing()) {
                    // 更新存储和内存信息
                    loadStorageInfo();
                    loadMemoryInfo();
                    
                    // 添加自动更新日志
                    addLogEntry(logLevel(LOG_LEVEL_DEBUG),
                            getString(R.string.dev_lab_log_category_auto_update),
                            getString(R.string.dev_lab_log_auto_refresh));
                    filterLogs();
                    
                    // 安排下次更新
                    updateHandler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        };
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }
    
    private void stopAutoUpdate() {
        if (updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (isAutoUpdateEnabled) {
            startAutoUpdate();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopAutoUpdate();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoUpdate();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private String logLevel(int index) {
        return getResources().getStringArray(R.array.dev_lab_log_levels)[index];
    }
    
    // 日志条目类
    public static class LogEntry {
        public String timestamp;
        public String level;
        public String tag;
        public String message;
    }
}
