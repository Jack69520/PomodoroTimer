package com.skyinit.pomodorotimer.ui.calendar;

import androidx.annotation.StringRes;

/**
 * 记录详情页一次性副作用。
 */
public final class SessionDetailEffect {

    public enum Type {
        SHOW_TOAST,
        NAVIGATE_BACK,
        OPEN_BLOCK_RECORDS
    }

    public final Type type;
    @StringRes
    public final int toastRes;
    public final long sessionStartTime;
    public final long sessionEndTime;

    private SessionDetailEffect(Type type,
                                @StringRes int toastRes,
                                long sessionStartTime,
                                long sessionEndTime) {
        this.type = type;
        this.toastRes = toastRes;
        this.sessionStartTime = sessionStartTime;
        this.sessionEndTime = sessionEndTime;
    }

    public static SessionDetailEffect showToast(@StringRes int resId) {
        return new SessionDetailEffect(Type.SHOW_TOAST, resId, 0L, 0L);
    }

    /** 可选先 Toast 再返回；toastRes 为 0 时仅返回。 */
    public static SessionDetailEffect navigateBack(@StringRes int toastRes) {
        return new SessionDetailEffect(Type.NAVIGATE_BACK, toastRes, 0L, 0L);
    }

    public static SessionDetailEffect openBlockRecords(long startTime, long endTime) {
        return new SessionDetailEffect(Type.OPEN_BLOCK_RECORDS, 0, startTime, endTime);
    }
}
