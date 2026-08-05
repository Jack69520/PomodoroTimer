package com.skyinit.pomodorotimer.domain.appidentity;

import android.content.Context;

import com.skyinit.pomodorotimer.util.AppLog;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 加载应用来源身份规则（assets 或 filesDir 更高 version 覆盖）。
 */
public final class AppIdentityRulesLoader {

    private static final String TAG = "AppIdentityRulesLoader";
    private static final String ASSET_FILE = "app_identity_rules.json";
    private static final String OVERRIDE_FILE = "app_identity_rules.json";

    private static volatile AppIdentityRulesLoader instance;

    private final AppIdentityConfig config;
    private final AppProvenanceResolver resolver;

    private AppIdentityRulesLoader(Context context) {
        config = parseConfig(loadConfigJson(context));
        resolver = new AppProvenanceResolver(config);
        AppLog.d(TAG, "Loaded app identity rules v" + config.version
                + ", oemPrefixes=" + config.oemPrefixes.size());
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (AppIdentityRulesLoader.class) {
                if (instance == null) {
                    instance = new AppIdentityRulesLoader(context.getApplicationContext());
                }
            }
        }
    }

    public static AppIdentityRulesLoader getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppIdentityRulesLoader not initialized");
        }
        return instance;
    }

    public static void reload(Context context) {
        synchronized (AppIdentityRulesLoader.class) {
            instance = new AppIdentityRulesLoader(context.getApplicationContext());
        }
    }

    public AppIdentityConfig getConfig() {
        return config;
    }

    public AppProvenanceResolver createResolver() {
        return resolver;
    }

    private JSONObject loadConfigJson(Context context) {
        try {
            JSONObject assetsConfig = readJson(context.getAssets().open(ASSET_FILE));
            File override = new File(context.getFilesDir(), OVERRIDE_FILE);
            if (override.exists()) {
                JSONObject overrideConfig = readJson(new FileInputStream(override));
                if (overrideConfig.optInt("version", 0) > assetsConfig.optInt("version", 0)) {
                    AppLog.d(TAG, "Using override app identity rules from internal storage");
                    return overrideConfig;
                }
            }
            return assetsConfig;
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to load app identity rules, using empty fallback", e);
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

    private AppIdentityConfig parseConfig(JSONObject root) {
        return new AppIdentityConfig(
                root.optInt("version", 1),
                toStringSet(root.optJSONArray("platformExact")),
                toStringSet(root.optJSONArray("googleExact")),
                toStringList(root.optJSONArray("platformPrefixes")),
                toStringList(root.optJSONArray("googlePrefixes")),
                toStringList(root.optJSONArray("oemPrefixes")),
                toStringSet(root.optJSONArray("oemServiceExact")),
                toStringSet(root.optJSONArray("oemPreloadExact"))
        );
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
}
