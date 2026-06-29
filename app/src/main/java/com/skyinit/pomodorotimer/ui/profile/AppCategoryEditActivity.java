package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.dao.BlockedAppDao;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.AppCategoryClassifier;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AppCategoryEditActivity extends BaseActivity {

    public static final String EXTRA_PACKAGE_NAME = "package_name";

    private static final String TAG = "AppCategoryEdit";

    private ImageView appIcon;
    private TextView appNameText;
    private TextView appPackageText;
    private TextView currentCategoryText;
    private Spinner categorySpinner;
    private Button btnSave;
    private Button btnResetAuto;

    private BlockedAppDao blockedAppDao;
    private String packageName;
    private BlockedApp blockedApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_category_edit);

        packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        if (packageName == null || packageName.isEmpty()) {
            Toast.makeText(this, R.string.blocking_toast_invalid_app, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initViews();
        blockedAppDao = AppDatabase.getDatabase(this).blockedAppDao();
        loadApp();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.blocking_category_edit_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initViews() {
        appIcon = findViewById(R.id.app_icon);
        appNameText = findViewById(R.id.app_name);
        appPackageText = findViewById(R.id.app_package);
        currentCategoryText = findViewById(R.id.current_category);
        categorySpinner = findViewById(R.id.category_spinner);
        btnSave = findViewById(R.id.btn_save);
        btnResetAuto = findViewById(R.id.btn_reset_auto);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, AppCategory.ASSIGNABLE);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveManualCategory());
        btnResetAuto.setOnClickListener(v -> resetToAutoCategory());
    }

    private void loadApp() {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                blockedApp = blockedAppDao.getBlockedAppByPackage(packageName);
                if (blockedApp == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.blocking_toast_app_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                runOnUiThread(() -> bindApp(blockedApp));
            } catch (Exception e) {
                AppLog.e(TAG, "Failed to load app", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.blocking_toast_load_failed, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void bindApp(BlockedApp app) {
        appNameText.setText(app.appName);
        appPackageText.setText(app.packageName);
        currentCategoryText.setText(app.category);
        currentCategoryText.setBackgroundResource(AppCategory.getBackgroundRes(app.category));

        setAppIcon(app.packageName);

        int selection = 0;
        for (int i = 0; i < AppCategory.ASSIGNABLE.length; i++) {
            if (AppCategory.ASSIGNABLE[i].equals(app.category)) {
                selection = i;
                break;
            }
        }
        categorySpinner.setSelection(selection);

        btnResetAuto.setEnabled(app.categoryManual);
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

    private void saveManualCategory() {
        String newCategory = (String) categorySpinner.getSelectedItem();
        if (newCategory == null) {
            finish();
            return;
        }
        if (newCategory.equals(blockedApp.category) && blockedApp.categoryManual) {
            finish();
            return;
        }

        AppExecutors.getInstance().diskIo(() -> {
            try {
                blockedAppDao.updateCategory(packageName, newCategory, true);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.blocking_toast_category_saved, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                AppLog.e(TAG, "Failed to save category", e);
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.blocking_toast_save_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void resetToAutoCategory() {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                String appName = pm.getApplicationLabel(appInfo).toString();
                String autoCategory = AppCategoryClassifier.classify(packageName, appName, appInfo);

                blockedAppDao.updateCategory(packageName, autoCategory, false);

                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.blocking_toast_auto_category_restored, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                AppLog.e(TAG, "Failed to reset category", e);
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.blocking_toast_restore_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    public static Intent createIntent(android.content.Context context, String packageName) {
        Intent intent = new Intent(context, AppCategoryEditActivity.class);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        return intent;
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
