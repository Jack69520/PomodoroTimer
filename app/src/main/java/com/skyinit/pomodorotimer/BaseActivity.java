package com.skyinit.pomodorotimer;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.skyinit.pomodorotimer.ui.theme.WallpaperApplier;
import com.skyinit.pomodorotimer.ui.theme.WallpaperCatalog;
import com.skyinit.pomodorotimer.ui.theme.WallpaperThemeRepository;
import com.skyinit.pomodorotimer.util.ColorContrastUtils;

/**
 * Activity 基类：壁纸背景 + 深色模式 + Toolbar/状态栏着色。
 * 子类通过 {@link #applyThemeStyle()} 指定 Material 主题，通过 {@link #onBarColorApplied(int)} 响应顶栏色。
 */
public class BaseActivity extends AppCompatActivity {

    @NonNull
    protected String currentWallpaperKey = WallpaperCatalog.DEFAULT_KEY;
    protected int currentNightMode;

    private int subpageBarColor = Color.TRANSPARENT;
    private boolean hasSubpageBarColor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyThemeStyle();
        super.onCreate(savedInstanceState);
        WallpaperApplier.preloadGradients(this);
        WallpaperThemeRepository repo = new WallpaperThemeRepository(this);
        currentWallpaperKey = repo.getSelectedKey();
        currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    @Override
    protected void onStart() {
        super.onStart();
        applyTheme();
    }

    protected void applyThemeStyle() {
        setTheme(R.style.Theme_PomodoroTimer);
    }

    /** Applies wallpaper to content + system bars only; brand tokens stay fixed. */
    public void applyTheme() {
        WallpaperThemeRepository repo = new WallpaperThemeRepository(this);
        WallpaperCatalog.WallpaperOption option = repo.getSelectedOption();
        currentWallpaperKey = option.key;
        WallpaperApplier.apply(this, option, this::onBarColorApplied);
    }

    protected void onBarColorApplied(int barColor) {
        applySubpageToolbarColors(barColor);
    }

    /**
     * 子页 Edge-to-Edge：状态栏落入 Toolbar 容器；导航栏 / 刘海落入 {@code R.id.subpage_content}。
     */
    protected void setupSubpageEdgeToEdge(@StringRes int titleRes) {
        setupSubpageEdgeToEdgeInternal(titleRes, false);
    }

    /**
     * 同 {@link #setupSubpageEdgeToEdge(int)}，内容区额外叠加 IME 底部 inset（账户表单页）。
     */
    protected void setupSubpageEdgeToEdgeWithIme(@StringRes int titleRes) {
        setupSubpageEdgeToEdgeInternal(titleRes, true);
    }

    private void setupSubpageEdgeToEdgeInternal(@StringRes int titleRes, boolean includeIme) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        MaterialToolbar toolbar = findViewById(R.id.subpage_toolbar);
        View toolbarContainer = findViewById(R.id.subpage_toolbar_container);
        View content = findViewById(R.id.subpage_content);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle(titleRes);
                getSupportActionBar().setElevation(0f);
            }
        }

        if (toolbarContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbarContainer, (view, windowInsets) -> {
                Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
                int top = Math.max(statusBars.top, cutout.top);
                view.setPadding(view.getPaddingLeft(), top, view.getPaddingRight(), view.getPaddingBottom());
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(toolbarContainer);
        }

        if (content != null) {
            applySubpageContentInsets(content, includeIme);
        }
    }

    private void applySubpageContentInsets(@NonNull View content, boolean includeIme) {
        final int initialLeft = content.getPaddingLeft();
        final int initialTop = content.getPaddingTop();
        final int initialRight = content.getPaddingRight();
        final int initialBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            int bottom = navigationBars.bottom;
            if (includeIme) {
                Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                bottom = Math.max(bottom, ime.bottom);
            }
            view.setPadding(
                    initialLeft + Math.max(navigationBars.left, cutout.left),
                    initialTop,
                    initialRight + Math.max(navigationBars.right, cutout.right),
                    initialBottom + bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    protected void applySubpageToolbarColors(int barColor) {
        View container = findViewById(R.id.subpage_toolbar_container);
        MaterialToolbar toolbar = findViewById(R.id.subpage_toolbar);
        if (container == null && toolbar == null) {
            return;
        }
        subpageBarColor = barColor;
        hasSubpageBarColor = true;
        if (container != null) {
            container.setBackgroundColor(barColor);
            container.setElevation(0f);
            container.setTranslationZ(0f);
        }
        if (toolbar != null) {
            toolbar.setBackground(null);
            toolbar.setElevation(0f);
            int titleColor = ColorContrastUtils.getContrastingTextColor(this, barColor);
            toolbar.setTitleTextColor(titleColor);
            toolbar.setNavigationIconTint(titleColor);
            if (toolbar.getOverflowIcon() != null) {
                toolbar.getOverflowIcon().setTint(titleColor);
            }
            if (toolbar.getMenu() != null) {
                tintSubpageMenuIcons(toolbar.getMenu(), titleColor);
            }
        }
    }

    protected void tintSubpageMenuIcons(@Nullable android.view.Menu menu) {
        if (!hasSubpageBarColor || menu == null) {
            return;
        }
        int titleColor = ColorContrastUtils.getContrastingTextColor(this, subpageBarColor);
        tintSubpageMenuIcons(menu, titleColor);
        MaterialToolbar toolbar = findViewById(R.id.subpage_toolbar);
        if (toolbar != null && toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(titleColor);
        }
    }

    private void tintSubpageMenuIcons(@NonNull android.view.Menu menu, int color) {
        for (int i = 0; i < menu.size(); i++) {
            android.view.MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().mutate().setTint(color);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WallpaperThemeRepository repo = new WallpaperThemeRepository(this);
        String latestKey = repo.getSelectedKey();
        int latestNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (!latestKey.equals(currentWallpaperKey) || latestNightMode != currentNightMode) {
            currentWallpaperKey = latestKey;
            currentNightMode = latestNightMode;
            recreate();
            return;
        }
        applyTheme();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int latestNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (latestNightMode != currentNightMode) {
            currentNightMode = latestNightMode;
            recreate();
            return;
        }
        applyTheme();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.setBackground(null);
        }
    }
}
