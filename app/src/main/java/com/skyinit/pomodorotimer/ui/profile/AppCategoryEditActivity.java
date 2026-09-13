package com.skyinit.pomodorotimer.ui.profile;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.util.AppCategory;

/**
 * 应用分类编辑页：仅渲染状态与转发意图。
 */
public class AppCategoryEditActivity extends SubpageActivity {

    public static final String EXTRA_PACKAGE_NAME = "package_name";

    private ImageView appIcon;
    private TextView appNameText;
    private TextView appPackageText;
    private TextView currentCategoryText;
    private Spinner categorySpinner;
    private MaterialButton btnSave;
    private MaterialButton btnResetAuto;

    private AppCategoryEditViewModel viewModel;
    private boolean syncingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_app_category_edit,
                R.string.blocking_category_edit_title);

        String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        if (packageName == null || packageName.isEmpty()) {
            Toast.makeText(this, R.string.blocking_toast_invalid_app, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();

        AppContainer container = AppContainer.getInstance(this);
        String activeUserId = container.getAccountManager().requireActiveUserId();
        viewModel = new ViewModelProvider(
                this,
                container.getViewModelFactory().createAppCategoryEditFactory(activeUserId, packageName)
        ).get(AppCategoryEditViewModel.class);

        setupSpinner();
        observeViewModel();
        viewModel.load();
    }

    private void initViews() {
        appIcon = findViewById(R.id.app_icon);
        appNameText = findViewById(R.id.app_name);
        appPackageText = findViewById(R.id.app_package);
        currentCategoryText = findViewById(R.id.current_category);
        categorySpinner = findViewById(R.id.category_spinner);
        btnSave = findViewById(R.id.btn_save);
        btnResetAuto = findViewById(R.id.btn_reset_auto);

        btnSave.setOnClickListener(v -> viewModel.saveManual());
        btnResetAuto.setOnClickListener(v -> viewModel.restoreAuto());
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, AppCategory.ASSIGNABLE);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (syncingSpinner) {
                    return;
                }
                viewModel.selectCategory(AppCategory.ASSIGNABLE[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, this::render);
        viewModel.getEffects().observe(this, this::handleEffect);
    }

    private void render(AppCategoryEditViewModel.UiState state) {
        if (state == null) {
            return;
        }
        boolean busy = state.loading || state.saving;
        btnSave.setEnabled(!busy);
        categorySpinner.setEnabled(!busy);

        BlockedApp app = state.app;
        if (app == null) {
            btnResetAuto.setEnabled(false);
            return;
        }

        appNameText.setText(app.appName);
        appPackageText.setText(app.packageName);
        currentCategoryText.setText(app.category);
        currentCategoryText.setBackgroundResource(AppCategory.getBackgroundRes(app.category));
        setAppIcon(app.packageName);

        btnResetAuto.setEnabled(!busy && app.categoryManual);

        String selected = state.selectedCategory != null ? state.selectedCategory : app.category;
        int selection = 0;
        for (int i = 0; i < AppCategory.ASSIGNABLE.length; i++) {
            if (AppCategory.ASSIGNABLE[i].equals(selected)) {
                selection = i;
                break;
            }
        }
        if (categorySpinner.getSelectedItemPosition() != selection) {
            syncingSpinner = true;
            categorySpinner.setSelection(selection);
            syncingSpinner = false;
        }
    }

    private void handleEffect(AppCategoryEditViewModel.Effect effect) {
        if (effect == null || effect.code == null) {
            return;
        }
        switch (effect.code) {
            case "NOT_FOUND":
                Toast.makeText(this, R.string.blocking_toast_app_not_found, Toast.LENGTH_SHORT).show();
                break;
            case "LOAD_FAILED":
                Toast.makeText(this, R.string.blocking_toast_load_failed, Toast.LENGTH_SHORT).show();
                break;
            case "SAVED":
                Toast.makeText(this, R.string.blocking_toast_category_saved, Toast.LENGTH_SHORT).show();
                break;
            case "SAVE_FAILED":
                Toast.makeText(this, R.string.blocking_toast_save_failed, Toast.LENGTH_SHORT).show();
                break;
            case "RESTORED":
                Toast.makeText(this, R.string.blocking_toast_auto_category_restored, Toast.LENGTH_SHORT).show();
                break;
            case "RESTORE_FAILED":
                Toast.makeText(this, R.string.blocking_toast_restore_failed, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        if (effect.finishOk) {
            if ("SAVED".equals(effect.code) || "RESTORED".equals(effect.code)) {
                setResult(RESULT_OK);
            }
            finish();
        }
    }

    private void setAppIcon(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
            Drawable icon = pm.getApplicationIcon(appInfo);
            appIcon.setImageDrawable(icon);
        } catch (Exception e) {
            appIcon.setImageResource(android.R.drawable.ic_menu_info_details);
        }
    }

    public static Intent createIntent(android.content.Context context, String packageName) {
        Intent intent = new Intent(context, AppCategoryEditActivity.class);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        return intent;
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
