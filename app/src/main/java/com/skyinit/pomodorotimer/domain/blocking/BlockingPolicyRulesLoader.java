package com.skyinit.pomodorotimer.domain.blocking;

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
 * 从 assets（或内部存储覆盖文件）加载屏蔽策略规则，支持离线热更新。
 * <p>
 * JSON 约定：以 {@code _readme}、{@code comment}、{@code note} 承载审查说明；
 * 包名列表支持扁平数组、{@code sections[]} 或 {@code packages[]} 三种格式。
 */
public final class BlockingPolicyRulesLoader {

    private static final String TAG = "BlockingPolicyRulesLoader";
    private static final String ASSET_FILE = "blocking_policy_rules.json";
    private static final String OVERRIDE_FILE = "blocking_policy_rules.json";

    private static volatile BlockingPolicyRulesLoader instance;

    private final BlockingPolicyConfig config;

    private BlockingPolicyRulesLoader(Context context) {
        config = parseConfig(loadConfigJson(context));
        AppLog.d(TAG, "Loaded blocking policy v" + config.version
                + ", critical=" + config.criticalApps.size()
                + ", whitelist=" + config.defaultWhitelist.size()
                + ", scanExact=" + config.scanIncludeExact.size());
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (BlockingPolicyRulesLoader.class) {
                if (instance == null) {
                    instance = new BlockingPolicyRulesLoader(context.getApplicationContext());
                }
            }
        }
    }

    public static BlockingPolicyRulesLoader getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BlockingPolicyRulesLoader not initialized");
        }
        return instance;
    }

    public static void reload(Context context) {
        synchronized (BlockingPolicyRulesLoader.class) {
            instance = new BlockingPolicyRulesLoader(context.getApplicationContext());
        }
    }

    public BlockingPolicyConfig getConfig() {
        return config;
    }

    public BlockingPolicyEngine createEngine() {
        return new BlockingPolicyEngine(config);
    }

    private JSONObject loadConfigJson(Context context) {
        try {
            JSONObject assetsConfig = readJson(context.getAssets().open(ASSET_FILE));
            File override = new File(context.getFilesDir(), OVERRIDE_FILE);
            if (override.exists()) {
                JSONObject overrideConfig = readJson(new FileInputStream(override));
                if (overrideConfig.optInt("version", 0) > assetsConfig.optInt("version", 0)) {
                    AppLog.d(TAG, "Using override blocking policy from internal storage");
                    return overrideConfig;
                }
            }
            return assetsConfig;
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to load blocking policy rules, using empty fallback", e);
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

    private BlockingPolicyConfig parseConfig(JSONObject root) {
        int version = root.optInt("version", 1);
        Set<String> criticalApps = parsePackageCollection(root.opt("criticalApps"));
        Set<String> defaultWhitelist = parsePackageCollection(root.opt("defaultWhitelist"));

        JSONObject scanInclude = root.optJSONObject("scanInclude");
        // 传入整个 scanInclude 对象，才能正确解析 sections[].packages（勿只传 sections 数组）
        Set<String> scanExact = scanInclude != null
                ? parsePackageCollection(scanInclude)
                : Collections.emptySet();
        List<PackageMatchRule> scanRules = scanInclude != null
                ? parseRuleList(scanInclude.optJSONArray("rules"))
                : Collections.emptyList();

        JSONObject whitelistMatchers = root.optJSONObject("whitelistMatchers");
        List<PackageMatchRule> whitelistRules = whitelistMatchers != null
                ? parseRuleList(whitelistMatchers.optJSONArray("rules"))
                : Collections.emptyList();

        List<BlockingPolicyConfig.AppTypeRule> appTypeRules = parseAppTypeRules(root);

        return new BlockingPolicyConfig(
                version,
                criticalApps,
                defaultWhitelist,
                scanExact,
                scanRules,
                whitelistRules,
                appTypeRules
        );
    }

    private List<BlockingPolicyConfig.AppTypeRule> parseAppTypeRules(JSONObject root) {
        Object node = root.opt("appTypeRules");
        if (node instanceof JSONObject) {
            return parseAppTypeRuleList(((JSONObject) node).optJSONArray("rules"));
        }
        return parseAppTypeRuleList(root.optJSONArray("appTypeRules"));
    }

    private List<BlockingPolicyConfig.AppTypeRule> parseAppTypeRuleList(JSONArray array) {
        List<BlockingPolicyConfig.AppTypeRule> rules = new ArrayList<>();
        if (array == null) {
            return rules;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            rules.add(new BlockingPolicyConfig.AppTypeRule(
                    item.optString("labelKey", ""),
                    parsePackageCollection(item.opt("exactPackages")),
                    toStringList(item.optJSONArray("packagePrefixes")),
                    toStringList(item.optJSONArray("packageContains")),
                    parsePackageCollection(item.opt("excludeExactPackages"))
            ));
        }
        return rules;
    }

    private List<PackageMatchRule> parseRuleList(JSONArray array) {
        List<PackageMatchRule> rules = new ArrayList<>();
        if (array == null) {
            return rules;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            rules.add(new PackageMatchRule(
                    parsePackageCollection(item.opt("exactPackages")),
                    toStringList(item.optJSONArray("packagePrefixes")),
                    toStringList(item.optJSONArray("packageContains")),
                    parsePackageCollection(item.opt("excludeExactPackages"))
            ));
        }
        return rules;
    }

    /**
     * 支持：字符串数组、带 sections/packages 的对象、或单个 section 数组。
     */
    private Set<String> parsePackageCollection(Object node) {
        if (node == null || node == JSONObject.NULL) {
            return Collections.emptySet();
        }
        if (node instanceof JSONArray) {
            return parsePackagesFromArray((JSONArray) node);
        }
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            Set<String> result = new HashSet<>();
            result.addAll(parsePackagesFromArray(obj.optJSONArray("packages")));
            JSONArray sections = obj.optJSONArray("sections");
            if (sections != null) {
                for (int i = 0; i < sections.length(); i++) {
                    JSONObject section = sections.optJSONObject(i);
                    if (section != null) {
                        result.addAll(parsePackagesFromArray(section.optJSONArray("packages")));
                    }
                }
            }
            return result;
        }
        return Collections.emptySet();
    }

    private Set<String> parsePackagesFromArray(JSONArray array) {
        if (array == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            Object entry = array.opt(i);
            String packageName = extractPackageId(entry);
            if (packageName != null && !packageName.isEmpty()) {
                result.add(packageName);
            }
        }
        return result;
    }

    private String extractPackageId(Object entry) {
        if (entry instanceof String) {
            return (String) entry;
        }
        if (entry instanceof JSONObject) {
            return ((JSONObject) entry).optString("id", "");
        }
        return null;
    }

    private Set<String> union(Set<String> left, Set<String> right) {
        Set<String> merged = new HashSet<>(left);
        merged.addAll(right);
        return merged;
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
