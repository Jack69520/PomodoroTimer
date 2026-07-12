package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.domain.blocking.AppTypeLabelResolver;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyConfig;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyRulesLoader;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.AppBlockingTestUtils;
import com.skyinit.pomodorotimer.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用屏蔽管理页（MVVM）：UI 只负责展示与事件转发，业务逻辑由 {@link AppBlockingViewModel} 处理。
 */
public class AppBlockingManagementActivity extends BaseActivity {
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

    private AppBlockingViewModel viewModel;
    private BlockedAppAdapter adapter;
    private final List<BlockedApp> filteredApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_blocking_management);

        setupToolbar();
        initViews();

        AppContainer container = AppContainer.getInstance(this);
        String activeUserId = container.getAccountManager().requireActiveUserId();
        viewModel = new ViewModelProvider(
                this,
                container.getViewModelFactory().createAppBlockingFactory(activeUserId)
        ).get(AppBlockingViewModel.class);

        BlockingPolicyConfig config = BlockingPolicyRulesLoader.getInstance().getConfig();
        adapter = new BlockedAppAdapter(
                filteredApps,
                new BlockedAppAdapter.OnAppToggleListener() {
                    @Override
                    public void onAppToggle(BlockedApp app, boolean isBlocked) {
                        viewModel.updateBlockingStatus(app, isBlocked);
                        showToggleToast(isBlocked, app.appName, true);
                    }

                    @Override
                    public void onWhitelistToggle(BlockedApp app, boolean isWhitelisted) {
                        viewModel.updateWhitelistStatus(app, isWhitelisted);
                        showToggleToast(isWhitelisted, app.appName, false);
                    }
                },
                BlockingPolicyRulesLoader.getInstance().createEngine(),
                new AppTypeLabelResolver(config)
        );
        adapter.setCategoryClickListener(app ->
                startActivityForResult(
                        AppCategoryEditActivity.createIntent(this, app.packageName),
                        REQUEST_EDIT_CATEGORY));

        setupRecyclerView();
        setupSearch();
        setupCategoryFilter();
        setupFilterButtons();
        updateButtonStates(false);
        setupScanButton();
        observeViewModel();

        viewModel.checkAutoScan();

        AppBlockingTestUtils.testPolicyLogic(this);
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

    private void setupRecyclerView() {
        appsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appsRecyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchButton.setOnClickListener(v ->
                viewModel.setSearchQuery(searchEditText.getText().toString()));
    }

    private void setupCategoryFilter() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, AppCategory.FILTER_OPTIONS);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setCategoryFilter(AppCategory.FILTER_OPTIONS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFilterButtons() {
        btnAllApps.setOnClickListener(v -> {
            viewModel.setShowWhitelistOnly(false);
            updateButtonStates(false);
        });

        btnWhitelist.setOnClickListener(v -> {
            viewModel.setShowWhitelistOnly(true);
            updateButtonStates(true);
        });
    }

    private void setupScanButton() {
        btnScanApps.setOnClickListener(v -> viewModel.scanInstalledApps());
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, state -> {
            if (state == null) {
                return;
            }

            if (progressBar != null) {
                progressBar.setVisibility(state.scanning ? View.VISIBLE : View.GONE);
            }

            filteredApps.clear();
            filteredApps.addAll(state.filteredApps);
            adapter.notifyDataSetChanged();
            updateEmptyState(state.filteredApps.isEmpty());

            totalAppsText.setText(getString(R.string.blocking_label_total_apps, state.totalCount));

            if (state.showWhitelistOnly) {
                blockedAppsText.setText(getString(R.string.blocking_label_whitelist_only, state.whitelistCount));
            } else {
                blockedAppsText.setText(getString(R.string.blocking_label_blocked_summary,
                        state.blockedCount, state.whitelistCount));
            }

            if (state.message != null) {
                if (state.messageIsError) {
                    Toast.makeText(this, R.string.blocking_toast_scan_failed, Toast.LENGTH_SHORT).show();
                } else if ("SCAN_HINT".equals(state.message)) {
                    Toast.makeText(this, R.string.blocking_toast_scan_hint, Toast.LENGTH_SHORT).show();
                } else if ("SCAN_NO_CHANGE".equals(state.message)) {
                    Toast.makeText(this, R.string.blocking_toast_scan_no_change, Toast.LENGTH_SHORT).show();
                } else if (state.message.startsWith("SCAN_COMPLETE:")) {
                    String[] parts = state.message.split(":");
                    int newCount = parts.length > 1 ? parseIntSafe(parts[1]) : 0;
                    int updatedCount = parts.length > 2 ? parseIntSafe(parts[2]) : 0;
                    String message = getString(R.string.blocking_toast_scan_complete);
                    if (newCount > 0) {
                        message += getString(R.string.blocking_toast_scan_new_apps, newCount);
                    }
                    if (updatedCount > 0) {
                        message += getString(R.string.blocking_toast_scan_updated_categories, updatedCount);
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
                viewModel.clearToast();
            }
        });
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateButtonStates(boolean showingWhitelist) {
        if (showingWhitelist) {
            btnAllApps.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            btnWhitelist.setTextColor(getResources().getColor(R.color.primary, getTheme()));
        } else {
            btnAllApps.setTextColor(getResources().getColor(R.color.primary, getTheme()));
            btnWhitelist.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
        }
    }

    private void updateEmptyState(boolean empty) {
        if (empty) {
            appsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            appsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void showToggleToast(boolean enabled, String appName, boolean blockingToggle) {
        String message;
        if (blockingToggle) {
            message = enabled
                    ? getString(R.string.blocking_toast_blocked_removed_whitelist, appName)
                    : getString(R.string.blocking_toast_unblocked_added_whitelist, appName);
        } else {
            message = enabled
                    ? getString(R.string.blocking_toast_whitelist_added, appName)
                    : getString(R.string.blocking_toast_whitelist_removed, appName);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_CATEGORY && resultCode == RESULT_OK) {
            // LiveData 会自动刷新
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
