package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.ui.theme.WallpaperCatalog;

/**
 * 主题色页用户意图。
 */
public final class ThemeColorIntent {

    public enum Type {
        REFRESH,
        SELECT_THEME,
        SELECT_CATEGORY
    }

    public final Type type;
    @Nullable
    public final String themeKey;
    @Nullable
    public final WallpaperCatalog.Category category;

    private ThemeColorIntent(Type type,
                             @Nullable String themeKey,
                             @Nullable WallpaperCatalog.Category category) {
        this.type = type;
        this.themeKey = themeKey;
        this.category = category;
    }

    @NonNull
    public static ThemeColorIntent refresh() {
        return new ThemeColorIntent(Type.REFRESH, null, null);
    }

    @NonNull
    public static ThemeColorIntent selectTheme(@NonNull String themeKey) {
        return new ThemeColorIntent(Type.SELECT_THEME, themeKey, null);
    }

    @NonNull
    public static ThemeColorIntent selectCategory(@NonNull WallpaperCatalog.Category category) {
        return new ThemeColorIntent(Type.SELECT_CATEGORY, null, category);
    }
}
