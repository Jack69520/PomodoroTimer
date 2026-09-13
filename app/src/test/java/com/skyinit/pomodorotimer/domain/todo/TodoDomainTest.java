package com.skyinit.pomodorotimer.domain.todo;

import com.skyinit.pomodorotimer.data.entity.TodoItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 待办领域层单测：零点、过期、重复推进、分组与筛选谓词。
 */
public class TodoDomainTest {

    @Test
    public void startOfDay_clearsTimeComponent() {
        long raw = System.currentTimeMillis();
        long day = DueDateTime.startOfDay(raw);
        assertEquals(0L, DueDateTime.startOfDay(0L));
        assertTrue(day <= raw);
        assertEquals(day, DueDateTime.startOfDay(day));
    }

    @Test
    public void overdue_requiresDueBeforeToday() {
        long today = DueDateTime.startOfToday();
        assertFalse(DueDateTime.isOverdue(0L, today));
        assertFalse(DueDateTime.isOverdue(today, today));
        assertTrue(DueDateTime.isOverdue(today - TimeUnit.DAYS.toMillis(1), today));
    }

    @Test
    public void nextDue_advancesPastToday() {
        long today = DueDateTime.startOfToday();
        long next = RecurrencePolicy.nextDueAfterCompletion(today, RecurrenceType.DAILY, today);
        assertTrue(next > today);
        assertEquals(today + TimeUnit.DAYS.toMillis(1), next);
    }

    @Test
    public void complete_recurring_rollsSameTask() {
        TodoItem item = new TodoItem("daily");
        item.dueDate = DueDateTime.startOfToday();
        item.recurrenceType = RecurrenceType.DAILY;
        item.completedPomodoros = 2;
        long today = DueDateTime.startOfToday();
        TodoCompletionPolicy.Result result =
                TodoCompletionPolicy.complete(item, System.currentTimeMillis(), today);
        assertTrue(result.rolledToNextOccurrence);
        assertFalse(result.markedCompleted);
        assertFalse(item.completed);
        assertEquals(0, item.completedPomodoros);
        assertTrue(item.dueDate > today);
    }

    @Test
    public void complete_oneShot_marksCompleted() {
        TodoItem item = new TodoItem("once");
        item.dueDate = DueDateTime.startOfToday();
        item.recurrenceType = RecurrenceType.NONE;
        long today = DueDateTime.startOfToday();
        TodoCompletionPolicy.Result result =
                TodoCompletionPolicy.complete(item, 12345L, today);
        assertTrue(result.markedCompleted);
        assertFalse(result.rolledToNextOccurrence);
        assertTrue(item.completed);
        assertEquals(12345L, item.completedTime);
    }

    @Test
    public void grouping_putsUndatedAndOverdueSeparately() {
        long today = DueDateTime.startOfToday();
        List<TodoItem> source = new ArrayList<>();
        TodoItem undated = new TodoItem("u");
        undated.dueDate = 0;
        TodoItem overdue = new TodoItem("o");
        overdue.dueDate = today - TimeUnit.DAYS.toMillis(1);
        TodoItem todayItem = new TodoItem("t");
        todayItem.dueDate = today;
        source.add(undated);
        source.add(overdue);
        source.add(todayItem);

        List<TodoGrouping.Section> sections = TodoGrouping.group(source, today);
        assertTrue(hasSection(sections, TodoSectionType.OVERDUE));
        assertTrue(hasSection(sections, TodoSectionType.TODAY));
        assertTrue(hasSection(sections, TodoSectionType.UNDATED));
    }

    @Test
    public void filter_overdue_excludesZeroAndToday() {
        long today = DueDateTime.startOfToday();
        List<TodoItem> source = new ArrayList<>();
        TodoItem zero = new TodoItem("z");
        zero.dueDate = 0;
        TodoItem todayItem = new TodoItem("t");
        todayItem.dueDate = today;
        TodoItem overdue = new TodoItem("o");
        overdue.dueDate = today - TimeUnit.DAYS.toMillis(2);
        source.add(zero);
        source.add(todayItem);
        source.add(overdue);

        List<TodoItem> filtered = TodoFilterQuery.apply(
                source,
                TodoFilterQuery.PriorityFilter.ALL,
                TodoFilterQuery.DueDateFilter.OVERDUE,
                null,
                "全部",
                today,
                false);
        assertEquals(1, filtered.size());
        assertEquals("o", filtered.get(0).title);
    }

    @Test
    public void sanitize_forcesNoneForCollection() {
        assertEquals(RecurrenceType.NONE,
                RecurrencePolicy.sanitizeForTask(RecurrenceType.DAILY, true));
        assertEquals(RecurrenceType.WEEKLY,
                RecurrencePolicy.sanitizeForTask(RecurrenceType.WEEKLY, false));
    }

    private static boolean hasSection(List<TodoGrouping.Section> sections, TodoSectionType type) {
        for (TodoGrouping.Section section : sections) {
            if (section.type == type && !section.items.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
