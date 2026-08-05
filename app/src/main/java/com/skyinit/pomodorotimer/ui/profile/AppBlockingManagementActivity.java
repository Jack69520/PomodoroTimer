package com.skyinit.pomodorotimer.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.domain.blocking.AppTypeLabelResolver;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyConfig;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyRulesLoader;
import com.skyinit.pomodorotimer.util.AppCategory;

/**
 * 应用屏蔽管理页：仅绑定 UI 与转发用户意图，业务状态由 {@link AppBlockingViewModel} 持有。
 */
public class AppBlockingManagementActivity extends BaseActivity {

    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private Spinner categorySpinner;
    private TextView btnAllApps;
    private TextView btnWhitelist;
    private TextView statTotalValue;
    private TextView statBlockedValue;
    private TextView statWhitelistValue;
    private MaterialButton btnScanApps;
    private MaterialButton btnEmptyScan;
    private RecyclerView appsRecyclerView;
    private LinearLayout emptyStateLayout;
    private TextView emptySubtitle;
    private ProgressBar progressBar;

    private AppBlockingViewModel viewModel;
    private BlockedAppAdapter adapter;

    private final ActivityResultLauncher<Intent> editCategoryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // LiveData 自动刷新列表
            });

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
                (app, isBlocked) -> viewModel.updateBlockingStatus(app, isBlocked),
                app -> editCategoryLauncher.launch(
                        AppCategoryEditActivity.createIntent(this, app.packageName)),
                BlockingPolicyRulesLoader.getInstance().createEngine(),
                new AppTypeLabelResolver(config)
        );

        setupRecyclerView();
        setupSearch();
        setupCategoryFilter();
        setupFilterSegments();
        setupScanButtons();
        observeViewModel();

        viewModel.checkAutoScan();
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
        clearSearchButton = findViewById(R.id.btn_clear_search);
        categorySpinner = findViewById(R.id.category_spinner);
        btnAllApps = findViewById(R.id.btn_all_apps);
        btnWhitelist = findViewById(R.id.btn_whitelist);
        statTotalValue = findViewById(R.id.stat_total_value);
        statBlockedValue = findViewById(R.id.stat_blocked_value);
        statWhitelistValue = findViewById(R.id.stat_whitelist_value);
        btnScanApps = findViewById(R.id.btn_scan_apps);
        btnEmptyScan = findViewById(R.id.btn_empty_scan);
        appsRecyclerView = findViewById(R.id.apps_recycler_view);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        emptySubtitle = findViewById(R.id.empty_state_subtitle);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        appsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appsRecyclerView.setHasFixedSize(true);
        appsRecyclerView.setItemAnimator(null);
        appsRecyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearSearchButton.setVisibility(
                        s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                viewModel.setSearchQueryDebounced(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.setSearchQueryImmediate(searchEditText.getText().toString());
                return true;
            }
            return false;
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            viewModel.setSearchQueryImmediate("");
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
                viewModel.setCategoryFilter(AppCategory.FILTER_OPTIONS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFilterSegments() {
        btnAllApps.setOnClickListener(v -> viewModel.setShowWhitelistOnly(false));
        btnWhitelist.setOnClickListener(v -> viewModel.setShowWhitelistOnly(true));
    }

    private void setupScanButtons() {
        View.OnClickListener scan = v -> {
            AppBlockingViewModel.UiState state = viewModel.getUiState().getValue();
            if (state != null && state.scanning) {
                return;
            }
            viewModel.scanInstalledApps();
        };
        btnScanApps.setOnClickListener(scan);
        btnEmptyScan.setOnClickListener(scan);
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, state -> {
            if (state == null) {
                return;
            }

            progressBar.setVisibility(state.scanning ? View.VISIBLE : View.GONE);
            btnScanApps.setEnabled(!state.scanning);
            btnEmptyScan.setEnabled(!state.scanning);
            btnScanApps.setText(state.scanning
                    ? R.string.blocking_scanning
                    : R.string.blocking_btn_scan);

            adapter.submitList(state.filteredApps);

            boolean empty = state.visibleCount == 0;
            appsRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) {
                if (!state.hasAnyApps) {
                    emptySubtitle.setText(R.string.blocking_empty_no_data);
                    btnEmptyScan.setVisibility(View.VISIBLE);
                } else {
                    emptySubtitle.setText(R.string.blocking_empty_search_hint);
                    btnEmptyScan.setVisibility(View.GONE);
                }
            }

            statTotalValue.setText(String.valueOf(state.totalCount));
            statBlockedValue.setText(String.valueOf(state.blockedCount));
            statWhitelistValue.setText(String.valueOf(state.whitelistCount));

            updateSegmentStates(state.showWhitelistOnly);
        });

        viewModel.getUiEvent().observe(this, event -> {
            if (event == null || event.code == null) {
                return;
            }
            showEventToast(event);
            viewModel.clearEvent();
        });
    }

    private void updateSegmentStates(boolean whitelistOnly) {
        btnAllApps.setSelected(!whitelistOnly);
        btnWhitelist.setSelected(whitelistOnly);
    }

    private void showEventToast(AppBlockingViewModel.UiEvent event) {
        String message;
        switch (event.code) {
            case "CRITICAL_LOCKED":
                message = getString(R.string.blocking_toast_critical_locked);
                break;
            case "SCAN_BUSY":
                message = getString(R.string.blocking_toast_scan_busy);
                break;
            case "SCAN_HINT":
                message = getString(R.string.blocking_toast_scan_hint);
                break;
            case "SCAN_NO_CHANGE":
                message = getString(R.string.blocking_toast_scan_no_change);
                break;
            case "SCAN_FAILED":
                message = getString(R.string.blocking_toast_scan_failed);
                break;
            case "SCAN_COMPLETE":
                message = getString(R.string.blocking_toast_scan_complete);
                if (event.newCount > 0) {
                    message += getString(R.string.blocking_toast_scan_new_apps, event.newCount);
                }
                if (event.updatedCount > 0) {
                    message += getString(R.string.blocking_toast_scan_updated_categories, event.updatedCount);
                }
                break;
            case "TOGGLE_BLOCKED":
                message = getString(R.string.blocking_toast_toggle_blocked,
                        event.appName != null ? event.appName : "");
                break;
            case "TOGGLE_ALLOWED":
                message = getString(R.string.blocking_toast_toggle_allowed,
                        event.appName != null ? event.appName : "");
                break;
            default:
                return;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
