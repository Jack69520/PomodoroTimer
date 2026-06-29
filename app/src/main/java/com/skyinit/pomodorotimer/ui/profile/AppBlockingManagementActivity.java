package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.BlockedAppDao;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.AppCategoryRulesLoader;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppScanner;
import com.skyinit.pomodorotimer.util.WhitelistManager;
import com.skyinit.pomodorotimer.R;
import android.os.Bundle;
import com.skyinit.pomodorotimer.util.AppBlockingTestUtils;
import com.skyinit.pomodorotimer.util.AppLog;
import android.content.Intent;
import android.widget.ProgressBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppBlockingManagementActivity extends BaseActivity {
    private static final String TAG = "AppBlockingManagement";
    private static final int REQUEST_EDIT_CATEGORY = 1001;
    
    private EditText searchEditText;
    private Button searchButton;
    private Spinner categorySpinner;
    private TextView totalAppsText;
    private TextView blockedAppsText;
    private Button btnAllApps;
    private Button btnWhitelist;
    private Button btnScanApps;
    private RecyclerView appsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    
    private AppDatabase database;
    private BlockedAppDao blockedAppDao;
    private BlockedAppAdapter adapter;
    private List<BlockedApp> allApps = new ArrayList<>();
    private List<BlockedApp> filteredApps = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentCategory = AppCategory.FILTER_ALL;
    private boolean isShowingWhitelist = false; // 当前是否显示白名单
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_blocking_management);

        setupToolbar();
        
        initViews();
        initData();
        setupRecyclerView();
        setupSearch();
        setupCategoryFilter();
        setupFilterButtons();
        setupScanButton();
        loadApps();
        
        // 首次启动时检查是否需要扫描应用
        checkAndScanAppsIfNeeded();
        
        AppBlockingTestUtils.testWhitelistLogic(this);
        AppBlockingTestUtils.testAppTypeRecognition(this);
    }
    
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.blocking_title_manage_apps);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void initViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.btn_search);
        categorySpinner = findViewById(R.id.category_spinner);
        totalAppsText = findViewById(R.id.total_apps_text);
        blockedAppsText = findViewById(R.id.blocked_apps_text);
        btnAllApps = findViewById(R.id.btn_all_apps);
        btnWhitelist = findViewById(R.id.btn_whitelist);
        btnScanApps = findViewById(R.id.btn_scan_apps);
        appsRecyclerView = findViewById(R.id.apps_recycler_view);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void initData() {
        database = AppDatabase.getDatabase(this);
        blockedAppDao = database.blockedAppDao();
    }
    
    private void setupRecyclerView() {
        adapter = new BlockedAppAdapter(filteredApps, new BlockedAppAdapter.OnAppToggleListener() {
            @Override
            public void onAppToggle(BlockedApp app, boolean isBlocked) {
                updateAppBlockingStatus(app, isBlocked);
            }

            @Override
            public void onWhitelistToggle(BlockedApp app, boolean isWhitelisted) {
                updateAppWhitelistStatus(app, isWhitelisted);
            }
        });
        
        adapter.setCategoryClickListener(app ->
                startActivityForResult(
                        AppCategoryEditActivity.createIntent(this, app.packageName),
                        REQUEST_EDIT_CATEGORY));

        appsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appsRecyclerView.setAdapter(adapter);
    }
    
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase();
                filterApps();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        searchButton.setOnClickListener(v -> {
            currentSearchQuery = searchEditText.getText().toString().toLowerCase();
            filterApps();
        });
    }
    
    private void setupCategoryFilter() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, AppCategory.FILTER_OPTIONS);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = AppCategory.FILTER_OPTIONS[position];
                filterApps();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void setupFilterButtons() {
        // 设置按钮点击监听器
        btnAllApps.setOnClickListener(v -> {
            isShowingWhitelist = false;
            updateButtonStates();
            updateStatistics();
            filterApps();
        });
        
        btnWhitelist.setOnClickListener(v -> {
            isShowingWhitelist = true;
            updateButtonStates();
            updateStatistics();
            filterApps();
        });
        
        // 初始化按钮状态
        updateButtonStates();
    }
    
    private void setupScanButton() {
        btnScanApps.setOnClickListener(v -> scanInstalledApps());
    }
    
    private void checkAndScanAppsIfNeeded() {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                int appCount = blockedAppDao.getAppCount();
                if (appCount == 0) {
                    // 数据库为空，自动扫描应用
                    runOnUiThread(() -> scanInstalledApps());
                } else {
                    // 即使数据库不为空，也检查是否有新应用需要扫描
                    // 这样可以确保新安装的应用能被发现
                    runOnUiThread(() -> {
                        // 显示提示信息，让用户知道可以手动扫描
                        Toast.makeText(this, R.string.blocking_toast_scan_hint, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                AppLog.e(TAG, "Error checking app count", e);
            }
        });
    }
    
    private void scanInstalledApps() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        AppExecutors.getInstance().diskIo(() -> {
            try {
                // 首先清理数据库中可能存在的预设应用数据
                cleanPresetApps();

                AppCategoryRulesLoader.reload(this);

                AppScanner scanner = new AppScanner(this);
                List<BlockedApp> scannedApps = scanner.scanInstalledApps();
                
                // 应用白名单配置
                WhitelistManager.applyWhitelistConfig(scannedApps);
                
                // 插入新应用，并更新已有应用的自动分类
                List<BlockedApp> newApps = new ArrayList<>();
                int updatedCount = 0;
                for (BlockedApp scannedApp : scannedApps) {
                    if (!blockedAppDao.appExists(scannedApp.packageName)) {
                        newApps.add(scannedApp);
                        continue;
                    }

                    BlockedApp existing = blockedAppDao.getBlockedAppByPackage(scannedApp.packageName);
                    if (existing == null) {
                        continue;
                    }

                    boolean changed = false;
                    if (!scannedApp.appName.equals(existing.appName)) {
                        existing.appName = scannedApp.appName;
                        changed = true;
                    }

                    if (!existing.categoryManual
                            && !scannedApp.category.equals(existing.category)) {
                        existing.category = scannedApp.category;
                        changed = true;
                        updatedCount++;
                    }

                    if (changed) {
                        blockedAppDao.update(existing);
                    }
                }

                if (!newApps.isEmpty()) {
                    blockedAppDao.insertAll(newApps);
                }

                final int finalUpdatedCount = updatedCount;
                final int finalNewCount = newApps.size();
                runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    String message;
                    if (finalNewCount == 0 && finalUpdatedCount == 0) {
                        message = getString(R.string.blocking_toast_scan_no_change);
                    } else {
                        message = getString(R.string.blocking_toast_scan_complete);
                        if (finalNewCount > 0) {
                            message += getString(R.string.blocking_toast_scan_new_apps, finalNewCount);
                        }
                        if (finalUpdatedCount > 0) {
                            message += getString(R.string.blocking_toast_scan_updated_categories, finalUpdatedCount);
                        }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadApps();
                });
                
            } catch (Exception e) {
                AppLog.e(TAG, "Error scanning apps", e);
                runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, R.string.blocking_toast_scan_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 清理数据库中可能存在的预设应用数据
     * 只保留设备上实际安装的应用
     */
    private void cleanPresetApps() {
        try {
            // 获取设备上实际安装的应用包名
            AppScanner scanner = new AppScanner(this);
            List<BlockedApp> installedApps = scanner.scanInstalledApps();
            Set<String> installedPackageNames = new HashSet<>();
            for (BlockedApp app : installedApps) {
                installedPackageNames.add(app.packageName);
            }
            
            // 获取数据库中的所有应用
            List<BlockedApp> allDbApps = blockedAppDao.getAllAppsSync();
            
            // 删除数据库中不在设备上的应用
            List<BlockedApp> appsToDelete = new ArrayList<>();
            for (BlockedApp dbApp : allDbApps) {
                if (!installedPackageNames.contains(dbApp.packageName)) {
                    appsToDelete.add(dbApp);
                }
            }
            
            if (!appsToDelete.isEmpty()) {
                for (BlockedApp app : appsToDelete) {
                    blockedAppDao.delete(app);
                }
                AppLog.d(TAG, "Cleaned " + appsToDelete.size() + " preset apps not installed on device");
            }
            
        } catch (Exception e) {
            AppLog.e(TAG, "Error cleaning preset apps", e);
        }
    }
    
    private void updateButtonStates() {
        // 根据当前模式更新按钮状态
        if (isShowingWhitelist) {
            btnAllApps.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            btnWhitelist.setTextColor(getResources().getColor(R.color.primary, getTheme()));
        } else {
            btnAllApps.setTextColor(getResources().getColor(R.color.primary, getTheme()));
            btnWhitelist.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
        }
    }
    
    private void loadApps() {
        // 从数据库加载已配置的应用
        blockedAppDao.getAllBlockedApps().observe(this, new Observer<List<BlockedApp>>() {
            @Override
            public void onChanged(List<BlockedApp> apps) {
                allApps.clear();
                allApps.addAll(apps);
                
                AppBlockingTestUtils.validateForcedLogic(allApps);
                
                filterApps();
                updateStatistics();
            }
        });
    }
    
    private void filterApps() {
        filteredApps.clear();
        
        for (BlockedApp app : allApps) {
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                app.appName.toLowerCase().contains(currentSearchQuery) ||
                app.packageName.toLowerCase().contains(currentSearchQuery);
            
            boolean matchesCategory = AppCategory.FILTER_ALL.equals(currentCategory)
                    || currentCategory.equals(app.category);
            
            // 根据按钮状态进行筛选
            boolean matchesButtonFilter;
            if (isShowingWhitelist) {
                matchesButtonFilter = app.isWhitelisted;
            } else {
                matchesButtonFilter = true; // 显示所有应用
            }
            
            if (matchesSearch && matchesCategory && matchesButtonFilter) {
                filteredApps.add(app);
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }
    
    private void updateStatistics() {
        int totalCount = allApps.size();
        int blockedCount = 0;
        int whitelistCount = 0;
        
        for (BlockedApp app : allApps) {
            if (app.isEnabled) {
                blockedCount++;
            }
            if (app.isWhitelisted) {
                whitelistCount++;
            }
        }
        
        totalAppsText.setText(getString(R.string.blocking_label_total_apps, totalCount));
        
        // 根据当前显示模式更新统计信息
        if (isShowingWhitelist) {
            blockedAppsText.setText(getString(R.string.blocking_label_whitelist_only, whitelistCount));
        } else {
            blockedAppsText.setText(getString(R.string.blocking_label_blocked_summary,
                    blockedCount, whitelistCount));
        }
    }
    
    private void updateEmptyState() {
        if (filteredApps.isEmpty()) {
            appsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            appsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void updateAppBlockingStatus(BlockedApp app, boolean isBlocked) {
        app.isEnabled = isBlocked;
        
        // 强制不允许既不加入白名单也不屏蔽
        if (!isBlocked) {
            // 取消屏蔽时自动加入白名单
            app.isWhitelisted = true;
        } else {
            // 启用屏蔽时自动移除白名单
            app.isWhitelisted = false;
        }
        
        AppExecutors.getInstance().diskIo(() -> {
            try {
                blockedAppDao.update(app);
                runOnUiThread(() -> {
                    // 更新统计
                    updateStatistics();
                    // 重新过滤应用列表以更新UI
                    filterApps();
                    // 显示提示
                    String message = isBlocked ?
                        getString(R.string.blocking_toast_blocked_removed_whitelist, app.appName) :
                        getString(R.string.blocking_toast_unblocked_added_whitelist, app.appName);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.blocking_toast_update_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateAppWhitelistStatus(BlockedApp app, boolean isWhitelisted) {
        app.isWhitelisted = isWhitelisted;
        
        // 强制不允许既不加入白名单也不屏蔽
        if (isWhitelisted) {
            // 加入白名单时自动取消屏蔽
            app.isEnabled = false;
        } else {
            // 从白名单移除时自动加入屏蔽
            app.isEnabled = true;
        }
        
        AppExecutors.getInstance().diskIo(() -> {
            try {
                blockedAppDao.update(app);
                runOnUiThread(() -> {
                    // 重新过滤应用列表以更新UI
                    filterApps();
                    // 更新统计信息
                    updateStatistics();
                    // 显示提示
                    String message = isWhitelisted ?
                        getString(R.string.blocking_toast_whitelist_added, app.appName) :
                        getString(R.string.blocking_toast_whitelist_removed, app.appName);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.blocking_toast_update_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_CATEGORY && resultCode == RESULT_OK) {
            loadApps();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
