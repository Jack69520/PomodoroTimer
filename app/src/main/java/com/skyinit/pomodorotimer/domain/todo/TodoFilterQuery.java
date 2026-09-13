package com.skyinit.pomodorotimer.domain.todo;

import com.skyinit.pomodorotimer.data.entity.TodoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 客户端筛选：优先级 × 截止日期窗口 × 分类；与过期谓词一致。
 */
public final class TodoFilterQuery {

    public enum PriorityFilter {
        ALL,
        LOW,
        MEDIUM,
        HIGH,
        URGENT;

        public int priorityValue() {
            switch (this) {
                case LOW:
                    return 0;
                case MEDIUM:
                    return 1;
                case HIGH:
                    return 2;
                case URGENT:
                    return 3;
                default:
                    return -1;
            }
        }
    }

    public enum DueDateFilter {
        ALL,
        TODAY,
        TOMORROW,
        NEXT_7_DAYS,
        NEXT_8_TO_14_DAYS,
        NEXT_30_DAYS,
        OVERDUE
    }

    private TodoFilterQuery() {
    }

    /**
     * @param categoryAllLabel 与 UI「全部」分类文案相等时视为不筛选分类
     */
    public static List<TodoItem> apply(List<TodoItem> source,
                                       PriorityFilter priorityFilter,
                                       DueDateFilter dueDateFilter,
                                       String category,
                                       String categoryAllLabel,
                                       long startOfToday,
                                       boolean includeCompleted) {
        List<TodoItem> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        boolean filterCategory = category != null
                && !category.isEmpty()
                && (categoryAllLabel == null || !categoryAllLabel.equals(category));

        for (TodoItem item : source) {
            if (item == null) {
                continue;
            }
            if (!includeCompleted && item.completed) {
                continue;
            }
            if (priorityFilter != null && priorityFilter != PriorityFilter.ALL) {
                if (item.priority != priorityFilter.priorityValue()) {
                    continue;
                }
            }
            if (filterCategory && (item.category == null || !item.category.equals(category))) {
                continue;
            }
            if (!matchesDue(item, dueDateFilter, startOfToday)) {
                continue;
            }
            out.add(item);
        }
        return out;
    }

    private static boolean matchesDue(TodoItem item, DueDateFilter filter, long startOfToday) {
        if (filter == null || filter == DueDateFilter.ALL) {
            return true;
        }
        long due = item.dueDate;
        long day = TimeUnit.DAYS.toMillis(1);
        switch (filter) {
            case OVERDUE:
                // 严格：有日期且早于今天（不含 due=0、不含今天零点）
                return !item.completed && DueDateTime.isOverdue(due, startOfToday);
            case TODAY:
                return DueDateTime.isToday(due, startOfToday);
            case TOMORROW:
                return DueDateTime.isTomorrow(due, startOfToday);
            case NEXT_7_DAYS:
                // [今天, 今天+7天)
                return due >= startOfToday && due < startOfToday + 7 * day;
            case NEXT_8_TO_14_DAYS:
                return due >= startOfToday + 7 * day && due < startOfToday + 14 * day;
            case NEXT_30_DAYS:
                return due >= startOfToday && due < startOfToday + 30 * day;
            default:
                return true;
        }
    }
}
