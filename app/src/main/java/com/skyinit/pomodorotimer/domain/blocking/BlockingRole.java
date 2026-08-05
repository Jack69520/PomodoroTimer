package com.skyinit.pomodorotimer.domain.blocking;

/**
 * 屏蔽策略角色。与 {@link com.skyinit.pomodorotimer.domain.appidentity.AppProvenance}、
 * 业务功能分类正交。
 */
public enum BlockingRole {
    /** 强制放行，UI 不可改为屏蔽 */
    CRITICAL,
    /** 默认放行，用户可改为屏蔽 */
    DEFAULT_ALLOW,
    /** 默认屏蔽，用户可加入白名单 */
    DEFAULT_BLOCK;

    public String toStorage() {
        return name();
    }

    public static BlockingRole fromStorage(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_BLOCK;
        }
        try {
            return BlockingRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            return DEFAULT_BLOCK;
        }
    }
}
