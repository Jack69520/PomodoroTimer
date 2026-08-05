package com.skyinit.pomodorotimer.domain.appidentity;

/**
 * 应用安装来源 / 平台属性。与屏蔽角色、业务功能分类正交。
 */
public enum AppProvenance {
    /** Android 平台核心（AOSP / framework） */
    PLATFORM,
    /** Google 应用与服务 */
    GOOGLE,
    /** 厂商系统服务（后台组件、账号、OTA 等） */
    OEM_SERVICE,
    /** 厂商预装应用（用户可打开的预装 UI） */
    OEM_PRELOAD,
    /** 用户安装的第三方应用 */
    THIRD_PARTY;

    public boolean skipsFuzzyCategoryRules() {
        return this == PLATFORM || this == GOOGLE || this == OEM_SERVICE;
    }

    public boolean isOem() {
        return this == OEM_SERVICE || this == OEM_PRELOAD;
    }

    public String toStorage() {
        return name();
    }

    public static AppProvenance fromStorage(String value) {
        if (value == null || value.isEmpty()) {
            return THIRD_PARTY;
        }
        try {
            return AppProvenance.valueOf(value);
        } catch (IllegalArgumentException e) {
            return THIRD_PARTY;
        }
    }
}
