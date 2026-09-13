package com.skyinit.pomodorotimer.ui.theme;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wallpaper skin catalog: stable string keys mapped to color/drawable resources.
 * Wallpaper tokens must not be used as component accents.
 */
public final class WallpaperCatalog {

    public static final String DEFAULT_KEY = "wallpaper_standard_00";

    public enum Category {
        STANDARD(R.string.settings_theme_category_standard),
        CHINESE(R.string.settings_theme_category_chinese),
        GRADIENT(R.string.settings_theme_category_gradient),
        MORANDI(R.string.settings_theme_category_morandi);

        @StringRes
        public final int titleRes;

        Category(@StringRes int titleRes) {
            this.titleRes = titleRes;
        }
    }

    public static final class WallpaperOption {
        @NonNull
        public final String key;
        public final int resId;
        @NonNull
        public final String name;
        @NonNull
        public final Category category;
        public final boolean gradient;

        WallpaperOption(@NonNull String key,
                        int resId,
                        @NonNull String name,
                        @NonNull Category category,
                        boolean gradient) {
            this.key = key;
            this.resId = resId;
            this.name = name;
            this.category = category;
            this.gradient = gradient;
        }
    }

    private static final String[] STANDARD_KEYS = {
            "wallpaper_standard_00",
            "wallpaper_standard_01", "wallpaper_standard_02", "wallpaper_standard_03",
            "wallpaper_standard_04", "wallpaper_standard_05", "wallpaper_standard_06",
            "wallpaper_standard_07", "wallpaper_standard_08", "wallpaper_standard_09",
            "wallpaper_standard_10"
    };
    private static final int[] STANDARD_RES = {
            R.color.wallpaper_standard_00,
            R.color.wallpaper_standard_01, R.color.wallpaper_standard_02, R.color.wallpaper_standard_03,
            R.color.wallpaper_standard_04, R.color.wallpaper_standard_05, R.color.wallpaper_standard_06,
            R.color.wallpaper_standard_07, R.color.wallpaper_standard_08, R.color.wallpaper_standard_09,
            R.color.wallpaper_standard_10
    };

    private static final String[] CHINESE_KEYS = {
            "wallpaper_chinese_01", "wallpaper_chinese_02", "wallpaper_chinese_03",
            "wallpaper_chinese_04", "wallpaper_chinese_05", "wallpaper_chinese_06",
            "wallpaper_chinese_07", "wallpaper_chinese_08", "wallpaper_chinese_09",
            "wallpaper_chinese_10"
    };
    private static final int[] CHINESE_RES = {
            R.color.wallpaper_chinese_01, R.color.wallpaper_chinese_02, R.color.wallpaper_chinese_03,
            R.color.wallpaper_chinese_04, R.color.wallpaper_chinese_05, R.color.wallpaper_chinese_06,
            R.color.wallpaper_chinese_07, R.color.wallpaper_chinese_08, R.color.wallpaper_chinese_09,
            R.color.wallpaper_chinese_10
    };

    private static final String[] GRADIENT_KEYS = {
            "wallpaper_gradient_01", "wallpaper_gradient_02", "wallpaper_gradient_03",
            "wallpaper_gradient_04", "wallpaper_gradient_05", "wallpaper_gradient_06",
            "wallpaper_gradient_07", "wallpaper_gradient_08", "wallpaper_gradient_09",
            "wallpaper_gradient_10"
    };
    private static final int[] GRADIENT_RES = {
            R.drawable.wallpaper_gradient_01, R.drawable.wallpaper_gradient_02,
            R.drawable.wallpaper_gradient_03, R.drawable.wallpaper_gradient_04,
            R.drawable.wallpaper_gradient_05, R.drawable.wallpaper_gradient_06,
            R.drawable.wallpaper_gradient_07, R.drawable.wallpaper_gradient_08,
            R.drawable.wallpaper_gradient_09, R.drawable.wallpaper_gradient_10
    };

    private static final String[] MORANDI_KEYS = {
            "wallpaper_morandi_01", "wallpaper_morandi_02", "wallpaper_morandi_03",
            "wallpaper_morandi_04", "wallpaper_morandi_05", "wallpaper_morandi_06",
            "wallpaper_morandi_07", "wallpaper_morandi_08", "wallpaper_morandi_09",
            "wallpaper_morandi_10", "wallpaper_morandi_11", "wallpaper_morandi_12",
            "wallpaper_morandi_13", "wallpaper_morandi_14", "wallpaper_morandi_15"
    };
    private static final int[] MORANDI_RES = {
            R.color.wallpaper_morandi_01, R.color.wallpaper_morandi_02, R.color.wallpaper_morandi_03,
            R.color.wallpaper_morandi_04, R.color.wallpaper_morandi_05, R.color.wallpaper_morandi_06,
            R.color.wallpaper_morandi_07, R.color.wallpaper_morandi_08, R.color.wallpaper_morandi_09,
            R.color.wallpaper_morandi_10, R.color.wallpaper_morandi_11, R.color.wallpaper_morandi_12,
            R.color.wallpaper_morandi_13, R.color.wallpaper_morandi_14, R.color.wallpaper_morandi_15
    };

    private WallpaperCatalog() {
    }

    @NonNull
    public static List<WallpaperOption> allOptions(@NonNull Context context) {
        Resources res = context.getResources();
        List<WallpaperOption> options = new ArrayList<>(46);
        append(options, STANDARD_KEYS, STANDARD_RES, res, R.array.wallpaper_standard_names,
                Category.STANDARD, false);
        append(options, CHINESE_KEYS, CHINESE_RES, res, R.array.wallpaper_chinese_names,
                Category.CHINESE, false);
        append(options, GRADIENT_KEYS, GRADIENT_RES, res, R.array.wallpaper_gradient_names,
                Category.GRADIENT, true);
        append(options, MORANDI_KEYS, MORANDI_RES, res, R.array.wallpaper_morandi_names,
                Category.MORANDI, false);
        return options;
    }

    @NonNull
    public static Map<String, WallpaperOption> indexByKey(@NonNull Context context) {
        Map<String, WallpaperOption> map = new LinkedHashMap<>();
        for (WallpaperOption option : allOptions(context)) {
            map.put(option.key, option);
        }
        return map;
    }

    @Nullable
    public static WallpaperOption findByKey(@NonNull Context context, @Nullable String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        for (WallpaperOption option : allOptions(context)) {
            if (option.key.equals(key)) {
                return option;
            }
        }
        return null;
    }

    @NonNull
    public static WallpaperOption requireOption(@NonNull Context context, @Nullable String key) {
        WallpaperOption option = findByKey(context, key);
        if (option != null) {
            return option;
        }
        WallpaperOption fallback = findByKey(context, DEFAULT_KEY);
        if (fallback != null) {
            return fallback;
        }
        return new WallpaperOption(
                DEFAULT_KEY,
                R.color.wallpaper_standard_00,
                context.getString(R.string.settings_theme_unknown),
                Category.STANDARD,
                false);
    }

    @NonNull
    public static String displayName(@NonNull Context context, @Nullable String key) {
        WallpaperOption option = findByKey(context, key);
        if (option != null) {
            return option.name;
        }
        return context.getString(R.string.settings_theme_unknown);
    }

    public static boolean isDefaultKey(@Nullable String key) {
        return key == null || key.isEmpty() || DEFAULT_KEY.equals(key);
    }

    private static void append(@NonNull List<WallpaperOption> out,
                               @NonNull String[] keys,
                               @NonNull int[] resIds,
                               @NonNull Resources resources,
                               @ArrayRes int namesArray,
                               @NonNull Category category,
                               boolean gradient) {
        String[] names = resources.getStringArray(namesArray);
        int count = Math.min(Math.min(keys.length, resIds.length), names.length);
        for (int i = 0; i < count; i++) {
            out.add(new WallpaperOption(keys[i], resIds[i], names[i], category, gradient));
        }
    }
}
