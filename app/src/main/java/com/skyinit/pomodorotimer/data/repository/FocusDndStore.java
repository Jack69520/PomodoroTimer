package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 专注勿扰「临时接管」状态的磁盘快照。
 * <p>
 * 与 {@link ActiveSessionStore} 解耦：会话清盘后仍需能恢复系统勿扰；
 * 本 Store 仅保存瞬态拥有权，不纳入用户偏好备份。
 * <p>
 * 写入一律 {@code commit()}，保证杀进程前落盘完成。
 */
public final class FocusDndStore {

    private static final String PREFS_NAME = "FocusDndPrefs";

    private static final String KEY_OWNED = "owned";
    private static final String KEY_SAVED_FILTER = "saved_filter";
    private static final String KEY_APPLIED_FILTER = "applied_filter";

    /** 无效 / 未写入的 filter 哨兵。 */
    public static final int FILTER_UNSET = Integer.MIN_VALUE;

    private FocusDndStore() {
    }

    /** 本 App 是否仍临时接管系统勿扰。 */
    public static boolean isOwned(Context context) {
        return prefs(context).getBoolean(KEY_OWNED, false);
    }

    /** 启用前采样到的原 interruption filter；未拥有时返回 {@link #FILTER_UNSET}。 */
    public static int getSavedFilter(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.getBoolean(KEY_OWNED, false)) {
            return FILTER_UNSET;
        }
        return prefs.getInt(KEY_SAVED_FILTER, FILTER_UNSET);
    }

    /** 本 App 写入的目标 filter（当前为 NONE）；未拥有时返回 {@link #FILTER_UNSET}。 */
    public static int getAppliedFilter(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.getBoolean(KEY_OWNED, false)) {
            return FILTER_UNSET;
        }
        return prefs.getInt(KEY_APPLIED_FILTER, FILTER_UNSET);
    }

    /**
     * 原子写入拥有权。须在调用 {@code setInterruptionFilter} 之前完成，
     * 避免「系统已改、磁盘未写」导致杀进程后无法恢复。
     */
    public static void saveOwnership(Context context, int savedFilter, int appliedFilter) {
        prefs(context).edit()
                .putBoolean(KEY_OWNED, true)
                .putInt(KEY_SAVED_FILTER, savedFilter)
                .putInt(KEY_APPLIED_FILTER, appliedFilter)
                .commit();
    }

    /** 清除拥有权标记与全部字段。 */
    public static void clear(Context context) {
        prefs(context).edit().clear().commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
