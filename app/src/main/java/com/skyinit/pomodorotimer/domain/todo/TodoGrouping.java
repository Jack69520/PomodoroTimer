package com.skyinit.pomodorotimer.domain.todo;

import com.skyinit.pomodorotimer.data.entity.TodoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 主页时间分组：将扁平列表拆成工作台各段。
 * <p>
 * 置顶任务同时出现在 PINNED 条，且仍保留在其日期分组中（PINNED 仅作快捷入口时可去重；
 * 本实现：PINNED 只含置顶未完成；日期组内仍含置顶项并优先排序）。
 */
public final class TodoGrouping {

    public static final class Section {
        public final TodoSectionType type;
        public final List<TodoItem> items;

        public Section(TodoSectionType type, List<TodoItem> items) {
            this.type = type;
            this.items = items != null ? items : Collections.emptyList();
        }
    }

    private static final int MAX_PINNED = 3;

    private TodoGrouping() {
    }

    public static int maxPinned() {
        return MAX_PINNED;
    }

    /**
     * @param source       全量待办（可含已完成）
     * @param startOfToday 今天零点
     */
    public static List<Section> group(List<TodoItem> source, long startOfToday) {
        List<TodoItem> pinned = new ArrayList<>();
        List<TodoItem> overdue = new ArrayList<>();
        List<TodoItem> today = new ArrayList<>();
        List<TodoItem> upcoming = new ArrayList<>();
        List<TodoItem> undated = new ArrayList<>();
        List<TodoItem> completed = new ArrayList<>();

        if (source != null) {
            for (TodoItem item : source) {
                if (item == null) {
                    continue;
                }
                if (item.completed) {
                    completed.add(item);
                    continue;
                }
                if (item.isPinned) {
                    pinned.add(item);
                }
                long due = item.dueDate;
                if (DueDateTime.hasNoDueDate(due)) {
                    undated.add(item);
                } else if (DueDateTime.isOverdue(due, startOfToday)) {
                    overdue.add(item);
                } else if (DueDateTime.isToday(due, startOfToday)) {
                    today.add(item);
                } else {
                    upcoming.add(item);
                }
            }
        }

        sortActive(pinned);
        sortActive(overdue);
        sortActive(today);
        sortActive(upcoming);
        sortActive(undated);
        sortCompleted(completed);

        // 置顶条最多展示 MAX_PINNED 条（已按置顶时间排好）
        if (pinned.size() > MAX_PINNED) {
            pinned = new ArrayList<>(pinned.subList(0, MAX_PINNED));
        }

        List<Section> sections = new ArrayList<>(6);
        if (!pinned.isEmpty()) {
            sections.add(new Section(TodoSectionType.PINNED, pinned));
        }
        if (!overdue.isEmpty()) {
            sections.add(new Section(TodoSectionType.OVERDUE, overdue));
        }
        if (!today.isEmpty()) {
            sections.add(new Section(TodoSectionType.TODAY, today));
        }
        if (!upcoming.isEmpty()) {
            sections.add(new Section(TodoSectionType.UPCOMING, upcoming));
        }
        if (!undated.isEmpty()) {
            sections.add(new Section(TodoSectionType.UNDATED, undated));
        }
        if (!completed.isEmpty()) {
            sections.add(new Section(TodoSectionType.COMPLETED, completed));
        }
        return sections;
    }

    /** 组内：置顶优先 → 优先级 → 创建时间。 */
    private static void sortActive(List<TodoItem> list) {
        Collections.sort(list, ACTIVE_COMPARATOR);
    }

    private static void sortCompleted(List<TodoItem> list) {
        Collections.sort(list, (a, b) -> {
            long t1 = a.completedTime > 0 ? a.completedTime : a.createdTime;
            long t2 = b.completedTime > 0 ? b.completedTime : b.createdTime;
            return Long.compare(t2, t1);
        });
    }

    private static final Comparator<TodoItem> ACTIVE_COMPARATOR = (item1, item2) -> {
        if (item1.isPinned != item2.isPinned) {
            return item1.isPinned ? -1 : 1;
        }
        if (item1.isPinned && item2.isPinned) {
            int byPinTime = Long.compare(item2.pinnedTime, item1.pinnedTime);
            if (byPinTime != 0) {
                return byPinTime;
            }
        }
        int byPriority = Integer.compare(item2.priority, item1.priority);
        if (byPriority != 0) {
            return byPriority;
        }
        return Long.compare(item2.createdTime, item1.createdTime);
    };
}
