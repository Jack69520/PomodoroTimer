package com.skyinit.pomodorotimer.domain.todo;

/**
 * 待办重复类型常量。
 * <p>
 * 仅普通待办允许非 {@link #NONE}；待办集必须为 NONE。
 */
public final class RecurrenceType {

    /** 不重复（一次性） */
    public static final int NONE = 0;
    /** 每日 */
    public static final int DAILY = 1;
    /** 每周 */
    public static final int WEEKLY = 2;
    /** 每月 */
    public static final int MONTHLY = 3;

    private RecurrenceType() {
    }

    /** 是否为有效的重复枚举值（含 NONE）。 */
    public static boolean isValid(int type) {
        return type >= NONE && type <= MONTHLY;
    }

    /** 是否启用了重复（非 NONE）。 */
    public static boolean isRecurring(int type) {
        return type > NONE && type <= MONTHLY;
    }
}
