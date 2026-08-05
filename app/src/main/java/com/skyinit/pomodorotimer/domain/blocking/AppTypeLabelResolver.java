package com.skyinit.pomodorotimer.domain.blocking;

import android.content.Context;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.domain.appidentity.AppProvenance;

/**
 * 将包名 / 来源解析为 UI 展示用的类型徽章（本地化字符串）。
 * 主徽章优先展示来源或屏蔽角色，避免与业务功能分类 chip 同名冲突。
 */
public final class AppTypeLabelResolver {

    private final BlockingPolicyConfig config;

    public AppTypeLabelResolver(BlockingPolicyConfig config) {
        this.config = config;
    }

    /**
     * 解析列表徽章：CRITICAL 优先；否则用来源；第三方再尝试功能型 type 规则（拨号等）。
     */
    public String resolve(Context context, String packageName, String provenanceStorage) {
        if (config.criticalApps.contains(packageName)) {
            return context.getString(R.string.app_type_system_critical);
        }

        AppProvenance provenance = AppProvenance.fromStorage(provenanceStorage);
        switch (provenance) {
            case PLATFORM:
                return context.getString(R.string.app_provenance_platform);
            case GOOGLE:
                return context.getString(R.string.app_provenance_google);
            case OEM_SERVICE:
                return context.getString(R.string.app_provenance_oem_service);
            case OEM_PRELOAD:
                return context.getString(R.string.app_provenance_oem_preload);
            case THIRD_PARTY:
            default:
                break;
        }

        for (BlockingPolicyConfig.AppTypeRule rule : config.appTypeRules) {
            if (rule.matches(packageName)) {
                String label = resolveLabel(context, rule.labelKey);
                // 忽略旧的「系统应用」类 labelKey，避免与业务分类混淆
                if (isDeprecatedSystemLabel(rule.labelKey)) {
                    continue;
                }
                return label;
            }
        }
        return context.getString(R.string.blocking_app_type_normal);
    }

    /** 兼容旧调用：无 provenance 时仅按规则与 critical 判定。 */
    public String resolve(Context context, String packageName) {
        return resolve(context, packageName, null);
    }

    private boolean isDeprecatedSystemLabel(String labelKey) {
        return "app_category_system".equals(labelKey) || "app_type_system".equals(labelKey);
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
