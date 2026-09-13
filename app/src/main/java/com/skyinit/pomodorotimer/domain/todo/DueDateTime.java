package com.skyinit.pomodorotimer.domain.todo;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 截止日期「日历日」工具：统一本地时区零点语义。
 * <p>
 * 约定：{@code dueDate == 0} 表示无日期；非 0 必须为当天 00:00:00.000。
 */
public final class DueDateTime {

    private DueDateTime() {
    }

    /** 将任意时间戳归一到本地日历日零点；输入 ≤0 原样返回。 */
    public static long startOfDay(long millis) {
        if (millis <= 0L) {
            return 0L;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        clearTime(calendar);
        return calendar.getTimeInMillis();
    }

    /** 今天本地零点。 */
    public static long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        clearTime(calendar);
        return calendar.getTimeInMillis();
    }

    /** 相对「今天零点」偏移若干整天后的零点。 */
    public static long startOfDayOffset(int dayOffset) {
        Calendar calendar = Calendar.getInstance();
        clearTime(calendar);
        calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
        return calendar.getTimeInMillis();
    }

    /** 是否无截止日期。 */
    public static boolean hasNoDueDate(long dueDate) {
        return dueDate <= 0L;
    }

    /**
     * 是否已过期：有日期、未完成语义由调用方保证 completed；
     * 此处只比较 dueDate 与今天零点。
     */
    public static boolean isOverdue(long dueDate, long startOfToday) {
        return dueDate > 0L && dueDate < startOfToday;
    }

    /** 是否为今天（等于今天零点）。 */
    public static boolean isToday(long dueDate, long startOfToday) {
        return dueDate > 0L && dueDate == startOfToday;
    }

    /** 是否为明天。 */
    public static boolean isTomorrow(long dueDate, long startOfToday) {
        return dueDate > 0L && dueDate == startOfToday + TimeUnit.DAYS.toMillis(1);
    }

    /** 相对今天已过期的整天数；非过期返回 0。 */
    public static int overdueDays(long dueDate, long startOfToday) {
        if (!isOverdue(dueDate, startOfToday)) {
            return 0;
        }
        long diff = startOfToday - dueDate;
        return (int) (diff / TimeUnit.DAYS.toMillis(1));
    }

    /**
     * 相对文案类型，供 UI 映射字符串资源。
     */
    public enum RelativeKind {
        NONE,
        TODAY,
        TOMORROW,
        OVERDUE,
        WEEKDAY_OR_DATE
    }

    /** 根据 dueDate 判定相对展示种类。 */
    public static RelativeKind relativeKind(long dueDate, long startOfToday) {
        if (hasNoDueDate(dueDate)) {
            return RelativeKind.NONE;
        }
        if (isOverdue(dueDate, startOfToday)) {
            return RelativeKind.OVERDUE;
        }
        if (isToday(dueDate, startOfToday)) {
            return RelativeKind.TODAY;
        }
        if (isTomorrow(dueDate, startOfToday)) {
            return RelativeKind.TOMORROW;
        }
        return RelativeKind.WEEKDAY_OR_DATE;
    }

    /** 格式化为 MM-dd；跨年时带 yyyy。 */
    public static String formatShortDate(long dueDate, long startOfToday) {
        if (hasNoDueDate(dueDate)) {
            return "";
        }
        Calendar due = Calendar.getInstance();
        due.setTimeInMillis(dueDate);
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(startOfToday);
        if (due.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
            return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    due.get(Calendar.YEAR),
                    due.get(Calendar.MONTH) + 1,
                    due.get(Calendar.DAY_OF_MONTH));
        }
        return String.format(Locale.getDefault(), "%02d-%02d",
                due.get(Calendar.MONTH) + 1,
                due.get(Calendar.DAY_OF_MONTH));
    }

    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
