package com.skyinit.pomodorotimer.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 assets（或本地覆盖文件）加载应用分类规则，并构建 O(1) 精确包名索引。
 */
public final class AppCategoryRulesLoader {

    private static final String TAG = "AppCategoryRulesLoader";
    private static final String ASSET_FILE = "app_category_rules.json";
    private static final String OVERRIDE_FILE = "app_category_rules.json";

    private static volatile AppCategoryRulesLoader instance;

    private final int version;
    private final List<CategoryRule> rulesInOrder;
    private final Map<String, String> exactPackageToCategory;
    private final Map<Integer, String> systemCategoryMap;

    private AppCategoryRulesLoader(Context context) {
        JSONObject root = loadConfigJson(context);
        version = root.optInt("version", 1);
        rulesInOrder = parseRules(root.optJSONArray("rules"));
        exactPackageToCategory = buildExactPackageIndex(rulesInOrder);
        systemCategoryMap = parseSystemCategoryMap(root.optJSONObject("systemCategoryMap"));
        AppLog.d(TAG, "Loaded rules v" + version + ", exactPackages=" + exactPackageToCategory.size());
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (AppCategoryRulesLoader.class) {
                if (instance == null) {
                    instance = new AppCategoryRulesLoader(context.getApplicationContext());
                }
            }
        }
    }

    public static AppCategoryRulesLoader getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppCategoryRulesLoader not initialized");
        }
        return instance;
    }

    /** 支持从应用内部存储热更新规则（版本号更高时覆盖 assets） */
    public static void reload(Context context) {
        synchronized (AppCategoryRulesLoader.class) {
            instance = new AppCategoryRulesLoader(context.getApplicationContext());
        }
    }

    public int getVersion() {
        return version;
    }

    public List<CategoryRule> getRulesInOrder() {
        return rulesInOrder;
    }

    public String getCategoryByExactPackage(String packageName) {
        return exactPackageToCategory.get(packageName);
    }

    public String mapSystemCategory(int systemCategory) {
        return systemCategoryMap.get(systemCategory);
    }

    private JSONObject loadConfigJson(Context context) {
        try {
            JSONObject assetsConfig = readJson(context.getAssets().open(ASSET_FILE));
            File override = new File(context.getFilesDir(), OVERRIDE_FILE);
            if (override.exists()) {
                JSONObject overrideConfig = readJson(new FileInputStream(override));
                if (overrideConfig.optInt("version", 0) > assetsConfig.optInt("version", 0)) {
                    AppLog.d(TAG, "Using override rules from internal storage");
                    return overrideConfig;
                }
            }
            return assetsConfig;
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to load category rules, using empty fallback", e);
            return new JSONObject();
        }
    }

    private JSONObject readJson(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return new JSONObject(sb.toString());
    }

    private List<CategoryRule> parseRules(JSONArray rulesArray) {
        List<CategoryRule> rules = new ArrayList<>();
        if (rulesArray == null) {
            return rules;
        }
        for (int i = 0; i < rulesArray.length(); i++) {
            JSONObject item = rulesArray.optJSONObject(i);
            if (item == null) {
                continue;
            }
            CategoryRule rule = new CategoryRule(
                    item.optString("category", AppCategory.OTHER),
                    toStringSet(item.optJSONArray("exactPackages")),
                    toStringList(item.optJSONArray("packagePrefixes")),
                    toStringList(item.optJSONArray("packageContains")),
                    toStringList(item.optJSONArray("appNameContains")),
                    item.optBoolean("skipSystemApp", true)
            );
            rules.add(rule);
        }
        return rules;
    }

    private Map<String, String> buildExactPackageIndex(List<CategoryRule> rules) {
        Map<String, String> index = new HashMap<>();
        for (CategoryRule rule : rules) {
            for (String pkg : rule.exactPackages) {
                index.put(pkg, rule.category);
            }
        }
        return index;
    }

    private Map<Integer, String> parseSystemCategoryMap(JSONObject mapObject) {
        Map<Integer, String> map = new HashMap<>();
        if (mapObject == null) {
            return map;
        }
        JSONArray names = mapObject.names();
        if (names == null) {
            return map;
        }
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            try {
                int systemCategory = Integer.parseInt(key);
                String category = mapObject.optString(key);
                if (AppCategory.isAssignable(category)) {
                    map.put(systemCategory, category);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid key
            }
        }
        return map;
    }

    private Set<String> toStringSet(JSONArray array) {
        if (array == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i);
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private List<String> toStringList(JSONArray array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i);
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    public static final class CategoryRule {
        public final String category;
        public final Set<String> exactPackages;
        public final List<String> packagePrefixes;
        public final List<String> packageContains;
        public final List<String> appNameContains;
        public final boolean skipSystemApp;

        public CategoryRule(String category,
                            Set<String> exactPackages,
                            List<String> packagePrefixes,
                            List<String> packageContains,
                            List<String> appNameContains,
                            boolean skipSystemApp) {
            this.category = category;
            this.exactPackages = exactPackages;
            this.packagePrefixes = packagePrefixes;
            this.packageContains = packageContains;
            this.appNameContains = appNameContains;
            this.skipSystemApp = skipSystemApp;
        }

        public boolean matches(String packageName, String appName, boolean isSystemApp) {
            if (skipSystemApp && isSystemApp) {
                return false;
            }
            for (String prefix : packagePrefixes) {
                if (packageName.startsWith(prefix)) {
                    return true;
                }
            }
            for (String keyword : packageContains) {
                if (packageName.contains(keyword)) {
                    return true;
                }
            }
            if (appName != null) {
                for (String keyword : appNameContains) {
                    if (appName.contains(keyword)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
