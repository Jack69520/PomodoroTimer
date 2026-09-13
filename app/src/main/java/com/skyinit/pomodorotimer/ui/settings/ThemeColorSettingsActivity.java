package com.skyinit.pomodorotimer.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.ui.theme.WallpaperCatalog;

/**
 * 主题色选择页：顶部分类 Tab + 压缩单选预览卡片。
 */
public class ThemeColorSettingsActivity extends SubpageActivity {

    private static final WallpaperCatalog.Category[] TAB_CATEGORIES = {
            WallpaperCatalog.Category.STANDARD,
            WallpaperCatalog.Category.CHINESE,
            WallpaperCatalog.Category.GRADIENT,
            WallpaperCatalog.Category.MORANDI
    };

    private ThemeColorViewModel viewModel;
    private ThemeColorAdapter adapter;
    private TabLayout categoryTabs;
    private RecyclerView themeList;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean suppressTabCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_theme_color_settings,
                R.string.settings_theme_color_title);

        viewModel = new ViewModelProvider(
                this,
                ((App) getApplication()).getContainer().getViewModelFactory()
        ).get(ThemeColorViewModel.class);

        categoryTabs = findViewById(R.id.theme_category_tabs);
        themeList = findViewById(R.id.theme_list);
        setupTabs();
        setupList();

        viewModel.getUiState().observe(this, this::render);
        viewModel.getEffects().observe(this, this::handleEffect);
    }

    private void setupTabs() {
        categoryTabs.removeAllTabs();
        for (WallpaperCatalog.Category category : TAB_CATEGORIES) {
            categoryTabs.addTab(categoryTabs.newTab().setText(category.titleRes));
        }
        categoryTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (suppressTabCallback || tab == null) {
                    return;
                }
                int index = tab.getPosition();
                if (index < 0 || index >= TAB_CATEGORIES.length) {
                    return;
                }
                viewModel.dispatch(ThemeColorIntent.selectCategory(TAB_CATEGORIES[index]));
                themeList.scrollToPosition(0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupList() {
        themeList.setLayoutManager(new LinearLayoutManager(this));
        themeList.setHasFixedSize(true);
        themeList.setItemAnimator(null);
        adapter = new ThemeColorAdapter(key ->
                viewModel.dispatch(ThemeColorIntent.selectTheme(key)));
        themeList.setAdapter(adapter);
    }

    private void render(@Nullable ThemeColorUiState state) {
        if (state == null || isFinishing() || isDestroyed()) {
            return;
        }
        syncTab(state.selectedCategory);
        adapter.submitList(state.items);
        themeList.setEnabled(!state.applying);
        categoryTabs.setEnabled(!state.applying);
    }

    private void syncTab(@Nullable WallpaperCatalog.Category category) {
        if (category == null) {
            return;
        }
        int target = indexOfCategory(category);
        if (target < 0) {
            return;
        }
        TabLayout.Tab tab = categoryTabs.getTabAt(target);
        if (tab == null || tab.isSelected()) {
            return;
        }
        suppressTabCallback = true;
        tab.select();
        suppressTabCallback = false;
    }

    private static int indexOfCategory(@NonNull WallpaperCatalog.Category category) {
        for (int i = 0; i < TAB_CATEGORIES.length; i++) {
            if (TAB_CATEGORIES[i] == category) {
                return i;
            }
        }
        return -1;
    }

    private void handleEffect(@Nullable ThemeColorEffect effect) {
        if (effect == null || isFinishing() || isDestroyed()) {
            return;
        }
        if (effect.type == ThemeColorEffect.Type.THEME_APPLIED) {
            if (effect.themeKey != null) {
                currentWallpaperKey = effect.themeKey;
            }
            applyTheme();
            mainHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, effect.toastRes, Toast.LENGTH_SHORT).show();
                }
            }, 280);
        } else if (effect.type == ThemeColorEffect.Type.THEME_FAILED) {
            Toast.makeText(this, effect.toastRes, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
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
