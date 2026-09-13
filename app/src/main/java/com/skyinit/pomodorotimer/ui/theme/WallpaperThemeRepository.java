package com.skyinit.pomodorotimer.ui.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.repository.SettingsManager;

/**
 * Single entry for reading/writing wallpaper theme preference + catalog resolve.
 */
public final class WallpaperThemeRepository {

    private final Context appContext;
    private final SettingsManager settingsManager;

    public WallpaperThemeRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.settingsManager = new SettingsManager(appContext);
    }

    public WallpaperThemeRepository(@NonNull Context context, @NonNull SettingsManager settingsManager) {
        this.appContext = context.getApplicationContext();
        this.settingsManager = settingsManager;
    }

    @NonNull
    public String getSelectedKey() {
        return settingsManager.getWallpaperThemeKey();
    }

    @NonNull
    public WallpaperCatalog.WallpaperOption getSelectedOption() {
        return WallpaperCatalog.requireOption(appContext, getSelectedKey());
    }

    /**
     * Persists wallpaper key. Returns resolved option (falls back to default on unknown key).
     */
    @NonNull
    public WallpaperCatalog.WallpaperOption setSelectedKey(@Nullable String key) {
        WallpaperCatalog.WallpaperOption option = WallpaperCatalog.requireOption(appContext, key);
        settingsManager.setWallpaperThemeKey(option.key);
        if (!WallpaperCatalog.isDefaultKey(option.key)) {
            settingsManager.setDarkModeOverride();
        }
        return option;
    }

    @NonNull
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}
