package com.skyinit.pomodorotimer.domain.blocking;

import android.content.Context;

import com.skyinit.pomodorotimer.R;

/**
 * 将包名解析为 UI 展示用的应用类型标签（本地化字符串）。
 */
public final class AppTypeLabelResolver {

    private final BlockingPolicyConfig config;

    public AppTypeLabelResolver(BlockingPolicyConfig config) {
        this.config = config;
    }

    public String resolve(Context context, String packageName) {
        for (BlockingPolicyConfig.AppTypeRule rule : config.appTypeRules) {
            if (rule.matches(packageName)) {
                return resolveLabel(context, rule.labelKey);
            }
        }
        return context.getString(R.string.blocking_app_type_normal);
    }

    private String resolveLabel(Context context, String labelKey) {
        if (labelKey == null || labelKey.isEmpty()) {
            return context.getString(R.string.blocking_app_type_normal);
        }
        int resId = context.getResources().getIdentifier(labelKey, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        return context.getString(R.string.blocking_app_type_normal);
    }
}
