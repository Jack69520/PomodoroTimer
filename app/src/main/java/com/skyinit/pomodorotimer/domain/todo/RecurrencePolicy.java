package com.skyinit.pomodorotimer.domain.todo;

import java.util.Calendar;

/**
 * 重复推进策略：从当前截止日期按类型滚动到「严格晚于今天」的下一期。
 */
public final class RecurrencePolicy {

    private RecurrencePolicy() {
    }

    /**
     * 计算下一期截止日期（本地零点）。
     *
     * @param currentDue   当前期 due（应已是零点）；若为 0 则从今天零点起算
     * @param recurrenceType {@link RecurrenceType}
     * @param startOfToday 今天零点
     * @return 下一期零点时间戳；若类型为 NONE 返回 currentDue
     */
    public static long nextDueAfterCompletion(long currentDue, int recurrenceType, long startOfToday) {
        if (!RecurrenceType.isRecurring(recurrenceType)) {
            return DueDateTime.startOfDay(currentDue);
        }
        // 无日期的重复任务：以今天为锚点开始推进
        long cursor = currentDue > 0L ? DueDateTime.startOfDay(currentDue) : startOfToday;
        // 至少推进一期；若仍不晚于今天则继续推进，避免完成当天又落在今天
        do {
            cursor = addOnePeriod(cursor, recurrenceType);
        } while (cursor <= startOfToday);
        return cursor;
    }

    /** 按日/周/月加一期。 */
    public static long addOnePeriod(long dueStartOfDay, int recurrenceType) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(DueDateTime.startOfDay(dueStartOfDay));
        switch (recurrenceType) {
            case RecurrenceType.WEEKLY:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case RecurrenceType.MONTHLY:
                calendar.add(Calendar.MONTH, 1);
                break;
            case RecurrenceType.DAILY:
            default:
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
        }
        return calendar.getTimeInMillis();
    }

    /**
     * 规范化写入前的重复字段：待办集强制 NONE；非法值回落 NONE。
     */
    public static int sanitizeForTask(int recurrenceType, boolean isCollection) {
        if (isCollection) {
            return RecurrenceType.NONE;
        }
        if (!RecurrenceType.isValid(recurrenceType)) {
            return RecurrenceType.NONE;
        }
        return recurrenceType;
    }
}
