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
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.domain.blocking.AppTypeLabelResolver;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyConfig;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyRulesLoader;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.util.AppCategory;

/**
 * 应用屏蔽管理页：仅绑定 UI 与转发用户意图，业务状态由 {@link AppBlockingViewModel} 持有。
 */
public class AppBlockingManagementActivity extends SubpageActivity {

    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private Spinner categorySpinner;
    private TextView btnFilterAll;
    private TextView btnFilterBlocked;
    private TextView btnFilterAllowed;
    private MaterialButton btnScanApps;
    private MaterialButton btnEmptyScan;
    private MaterialButton btnEmptyResetFilters;
    private RecyclerView appsRecyclerView;
    private LinearLayout emptyStateLayout;
    private TextView emptySubtitle;
    private ProgressBar progressBar;

    private AppBlockingViewModel viewModel;
    private BlockedAppAdapter adapter;
    private boolean syncingSearchFromState;
    private boolean syncingCategoryFromState;

    private final ActivityResultLauncher<Intent> editCategoryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Room LiveData 自动刷新列表
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_app_blocking_management,
                R.string.blocking_title_manage_apps);

        initViews();

        AppContainer container = AppContainer.getInstance(this);
        String activeUserId = container.getAccountManager().requireActiveUserId();
        viewModel = new ViewModelProvider(
                this,
                container.getViewModelFactory().createAppBlockingFactory(activeUserId)
        ).get(AppBlockingViewModel.class);

        BlockingPolicyConfig config = BlockingPolicyRulesLoader.getInstance().getConfig();
        adapter = new BlockedAppAdapter(
                (app, isBlocked) -> viewModel.dispatch(AppBlockingIntent.toggle(app, isBlocked)),
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

        viewModel.dispatch(AppBlockingIntent.checkAutoScan());
    }

    private void initViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        clearSearchButton = findViewById(R.id.btn_clear_search);
        categorySpinner = findViewById(R.id.category_spinner);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterBlocked = findViewById(R.id.btn_filter_blocked);
        btnFilterAllowed = findViewById(R.id.btn_filter_allowed);
        btnScanApps = findViewById(R.id.btn_scan_apps);
        btnEmptyScan = findViewById(R.id.btn_empty_scan);
        btnEmptyResetFilters = findViewById(R.id.btn_empty_reset_filters);
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
                if (syncingSearchFromState) {
                    return;
                }
                viewModel.dispatch(AppBlockingIntent.searchDebounced(s != null ? s.toString() : ""));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.dispatch(AppBlockingIntent.searchImmediate(
                        searchEditText.getText().toString()));
                return true;
            }
            return false;
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            viewModel.dispatch(AppBlockingIntent.searchImmediate(""));
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
                if (syncingCategoryFromState) {
                    return;
                }
                viewModel.dispatch(AppBlockingIntent.setCategoryFilter(
                        AppCategory.FILTER_OPTIONS[position]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFilterSegments() {
        btnFilterAll.setOnClickListener(v ->
                viewModel.dispatch(AppBlockingIntent.setStatusFilter(
                        AppBlockingIntent.StatusFilter.ALL)));
        btnFilterBlocked.setOnClickListener(v ->
                viewModel.dispatch(AppBlockingIntent.setStatusFilter(
                        AppBlockingIntent.StatusFilter.BLOCKED)));
        btnFilterAllowed.setOnClickListener(v ->
                viewModel.dispatch(AppBlockingIntent.setStatusFilter(
                        AppBlockingIntent.StatusFilter.ALLOWED)));
    }

    private void setupScanButtons() {
        View.OnClickListener scan = v -> viewModel.dispatch(AppBlockingIntent.scan());
        btnScanApps.setOnClickListener(scan);
        btnEmptyScan.setOnClickListener(scan);
        btnEmptyResetFilters.setOnClickListener(v -> {
            syncingSearchFromState = true;
            searchEditText.setText("");
            syncingSearchFromState = false;
            clearSearchButton.setVisibility(View.GONE);
            viewModel.dispatch(AppBlockingIntent.resetFilters());
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, this::render);
        viewModel.getUiEvent().observe(this, this::showEventToast);
    }

    private void render(AppBlockingViewModel.UiState state) {
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
                btnEmptyResetFilters.setVisibility(View.GONE);
            } else {
                emptySubtitle.setText(R.string.blocking_empty_search_hint);
                btnEmptyScan.setVisibility(View.GONE);
                boolean filtersActive = (state.searchQuery != null && !state.searchQuery.isEmpty())
                        || (state.categoryFilter != null
                        && !AppCategory.FILTER_ALL.equals(state.categoryFilter))
                        || state.statusFilter != AppBlockingIntent.StatusFilter.ALL;
                btnEmptyResetFilters.setVisibility(filtersActive ? View.VISIBLE : View.GONE);
            }
        }

        btnFilterAll.setText(getString(R.string.blocking_segment_all, state.totalCount));
        btnFilterBlocked.setText(getString(R.string.blocking_segment_blocked, state.blockedCount));
        btnFilterAllowed.setText(getString(R.string.blocking_segment_allowed, state.allowedCount));
        updateSegmentStates(state.statusFilter);

        syncCategorySpinner(state.categoryFilter);
        syncSearchField(state.searchQuery);
    }

    private void syncCategorySpinner(String category) {
        if (category == null) {
            return;
        }
        int index = 0;
        for (int i = 0; i < AppCategory.FILTER_OPTIONS.length; i++) {
            if (AppCategory.FILTER_OPTIONS[i].equals(category)) {
                index = i;
                break;
            }
        }
        if (categorySpinner.getSelectedItemPosition() != index) {
            syncingCategoryFromState = true;
            categorySpinner.setSelection(index);
            syncingCategoryFromState = false;
        }
    }

    private void syncSearchField(String query) {
        String display = query != null ? query : "";
        String current = searchEditText.getText() != null
                ? searchEditText.getText().toString() : "";
        if (display.isEmpty() && !current.isEmpty() && !searchEditText.hasFocus()) {
            syncingSearchFromState = true;
            searchEditText.setText("");
            syncingSearchFromState = false;
            clearSearchButton.setVisibility(View.GONE);
        }
    }

    private void updateSegmentStates(AppBlockingIntent.StatusFilter filter) {
        btnFilterAll.setSelected(filter == AppBlockingIntent.StatusFilter.ALL);
        btnFilterBlocked.setSelected(filter == AppBlockingIntent.StatusFilter.BLOCKED);
        btnFilterAllowed.setSelected(filter == AppBlockingIntent.StatusFilter.ALLOWED);
    }

    private void showEventToast(AppBlockingViewModel.UiEvent event) {
        if (event == null || event.code == null) {
            return;
        }
        String message;
        switch (event.code) {
            case "SCAN_BUSY":
                message = getString(R.string.blocking_toast_scan_busy);
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
                    message += getString(R.string.blocking_toast_scan_updated_categories,
                            event.updatedCount);
                }
                if (event.removedCount > 0) {
                    message += getString(R.string.blocking_toast_scan_removed_apps,
                            event.removedCount);
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
