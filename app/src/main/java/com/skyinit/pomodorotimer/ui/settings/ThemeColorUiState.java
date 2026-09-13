package com.skyinit.pomodorotimer.ui.settings;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import com.skyinit.pomodorotimer.ui.theme.WallpaperCatalog;

/**
 * 主题色页不可变 UI 快照（按分类 Tab 过滤后的卡片列表）。
 */
public final class ThemeColorUiState {

    public static final class Item {
        @NonNull
        public final String key;
        public final int resId;
        @NonNull
        public final String name;
        @NonNull
        public final WallpaperCatalog.Category category;
        public final boolean gradient;
        public final boolean selected;

        public Item(@NonNull String key,
                    int resId,
                    @NonNull String name,
                    @NonNull WallpaperCatalog.Category category,
                    boolean gradient,
                    boolean selected) {
            this.key = key;
            this.resId = resId;
            this.name = name;
            this.category = category;
            this.gradient = gradient;
            this.selected = selected;
        }

        @NonNull
        public static Item option(@NonNull WallpaperCatalog.WallpaperOption option, boolean selected) {
            return new Item(option.key, option.resId, option.name, option.category,
                    option.gradient, selected);
        }
    }

    @NonNull
    public final String selectedKey;
    @NonNull
    public final WallpaperCatalog.Category selectedCategory;
    public final boolean applying;
    @NonNull
    public final List<Item> items;

    public ThemeColorUiState(@NonNull String selectedKey,
                             @NonNull WallpaperCatalog.Category selectedCategory,
                             boolean applying,
                             @NonNull List<Item> items) {
        this.selectedKey = selectedKey;
        this.selectedCategory = selectedCategory;
        this.applying = applying;
        this.items = Collections.unmodifiableList(items);
    }
}
