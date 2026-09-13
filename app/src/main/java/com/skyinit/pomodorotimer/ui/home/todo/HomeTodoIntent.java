package com.skyinit.pomodorotimer.ui.home.todo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.domain.todo.TodoFilterQuery;

/**
 * 首页待办工作台用户意图。
 */
public final class HomeTodoIntent {

    public enum Type {
        START,
        REFRESH_DAY_BOUNDARY,
        TOGGLE_FILTER_PANEL,
        SET_PRIORITY_FILTER,
        SET_DUE_DATE_FILTER,
        SET_CATEGORY_FILTER,
        CLEAR_FILTERS,
        TOGGLE_COMPLETE,
        TOGGLE_PIN,
        DELETE,
        CONFIRM_DELETE,
        CANCEL_DELETE,
        REQUEST_RESCHEDULE,
        RESCHEDULE_TO,
        RESCHEDULE_TO_TODAY,
        TOGGLE_COMPLETED_SECTION,
        OPEN_EDIT,
        START_TIMER,
        SWIPE_DELETE,
        TOGGLE_COLLECTION_EXPAND,
        TOGGLE_SUBTASK_COMPLETE,
        START_NEXT_SUBTASK,
        START_SUBTASK_TIMER,
        REQUEST_DELETE_SUBTASK,
        CONFIRM_DELETE_SUBTASK,
        CANCEL_DELETE_SUBTASK,
        SWIPE_DELETE_SUBTASK
    }

    public final Type type;
    public final int todoId;
    public final long dueMillis;
    public final int subtaskId;
    public final int parentId;
    public final boolean checked;
    @Nullable
    public final TodoItem todo;
    @Nullable
    public final SubTask subTask;
    @Nullable
    public final TodoFilterQuery.PriorityFilter priorityFilter;
    @Nullable
    public final TodoFilterQuery.DueDateFilter dueDateFilter;
    @Nullable
    public final String categoryFilter;

    private HomeTodoIntent(Type type,
                           int todoId,
                           long dueMillis,
                           int subtaskId,
                           int parentId,
                           boolean checked,
                           @Nullable TodoItem todo,
                           @Nullable SubTask subTask,
                           @Nullable TodoFilterQuery.PriorityFilter priorityFilter,
                           @Nullable TodoFilterQuery.DueDateFilter dueDateFilter,
                           @Nullable String categoryFilter) {
        this.type = type;
        this.todoId = todoId;
        this.dueMillis = dueMillis;
        this.subtaskId = subtaskId;
        this.parentId = parentId;
        this.checked = checked;
        this.todo = todo;
        this.subTask = subTask;
        this.priorityFilter = priorityFilter;
        this.dueDateFilter = dueDateFilter;
        this.categoryFilter = categoryFilter;
    }

    private static HomeTodoIntent simple(Type type) {
        return new HomeTodoIntent(type, 0, 0L, 0, 0, false, null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent start() {
        return simple(Type.START);
    }

    @NonNull
    public static HomeTodoIntent refreshDayBoundary() {
        return simple(Type.REFRESH_DAY_BOUNDARY);
    }

    @NonNull
    public static HomeTodoIntent toggleFilterPanel() {
        return simple(Type.TOGGLE_FILTER_PANEL);
    }

    @NonNull
    public static HomeTodoIntent setPriorityFilter(@NonNull TodoFilterQuery.PriorityFilter filter) {
        return new HomeTodoIntent(Type.SET_PRIORITY_FILTER, 0, 0L, 0, 0, false,
                null, null, filter, null, null);
    }

    @NonNull
    public static HomeTodoIntent setDueDateFilter(@NonNull TodoFilterQuery.DueDateFilter filter) {
        return new HomeTodoIntent(Type.SET_DUE_DATE_FILTER, 0, 0L, 0, 0, false,
                null, null, null, filter, null);
    }

    @NonNull
    public static HomeTodoIntent setCategoryFilter(@Nullable String category) {
        return new HomeTodoIntent(Type.SET_CATEGORY_FILTER, 0, 0L, 0, 0, false,
                null, null, null, null, category);
    }

    @NonNull
    public static HomeTodoIntent clearFilters() {
        return simple(Type.CLEAR_FILTERS);
    }

    @NonNull
    public static HomeTodoIntent toggleComplete(int todoId) {
        return new HomeTodoIntent(Type.TOGGLE_COMPLETE, todoId, 0L, 0, 0, false,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent togglePin(int todoId) {
        return new HomeTodoIntent(Type.TOGGLE_PIN, todoId, 0L, 0, 0, false,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent delete(int todoId) {
        return new HomeTodoIntent(Type.DELETE, todoId, 0L, 0, 0, false,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent confirmDelete() {
        return simple(Type.CONFIRM_DELETE);
    }

    @NonNull
    public static HomeTodoIntent cancelDelete() {
        return simple(Type.CANCEL_DELETE);
    }

    @NonNull
    public static HomeTodoIntent requestReschedule(int todoId) {
        return new HomeTodoIntent(Type.REQUEST_RESCHEDULE, todoId, 0L, 0, 0, false,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent rescheduleTo(int todoId, long dueMillis) {
        return new HomeTodoIntent(Type.RESCHEDULE_TO, todoId, dueMillis, 0, 0, false,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent rescheduleToToday(int todoId) {
        return new HomeTodoIntent(Type.RESCHEDULE_TO_TODAY, todoId, 0L, 0, 0, false,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent toggleCompletedSection() {
        return simple(Type.TOGGLE_COMPLETED_SECTION);
    }

    @NonNull
    public static HomeTodoIntent openEdit(@NonNull TodoItem todo) {
        return new HomeTodoIntent(Type.OPEN_EDIT, todo.id, 0L, 0, 0, false,
                todo, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent startTimer(@NonNull TodoItem todo) {
        return new HomeTodoIntent(Type.START_TIMER, todo.id, 0L, 0, 0, false,
                todo, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent swipeDelete(@NonNull TodoItem todo) {
        return new HomeTodoIntent(Type.SWIPE_DELETE, todo.id, 0L, 0, 0, false,
                todo, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent toggleCollectionExpand(int collectionId) {
        return new HomeTodoIntent(Type.TOGGLE_COLLECTION_EXPAND, collectionId, 0L, 0, 0, false,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent toggleSubtaskComplete(int subtaskId, int parentId, boolean checked) {
        return new HomeTodoIntent(Type.TOGGLE_SUBTASK_COMPLETE, 0, 0L, subtaskId, parentId, checked,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent startNextSubtask(int collectionId) {
        return new HomeTodoIntent(Type.START_NEXT_SUBTASK, collectionId, 0L, 0, 0, false,
                null, null, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent startSubtaskTimer(@NonNull TodoItem parent, @NonNull SubTask subTask) {
        return new HomeTodoIntent(Type.START_SUBTASK_TIMER, parent.id, 0L, subTask.id, parent.id, false,
                parent, subTask, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent requestDeleteSubtask(@NonNull SubTask subTask) {
        return new HomeTodoIntent(Type.REQUEST_DELETE_SUBTASK, 0, 0L, subTask.id, subTask.parentTaskId,
                false, null, subTask, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent swipeDeleteSubtask(@NonNull SubTask subTask) {
        return new HomeTodoIntent(Type.SWIPE_DELETE_SUBTASK, 0, 0L, subTask.id, subTask.parentTaskId,
                false, null, subTask, null, null, null);
    }

    @NonNull
    public static HomeTodoIntent confirmDeleteSubtask() {
        return simple(Type.CONFIRM_DELETE_SUBTASK);
    }

    @NonNull
    public static HomeTodoIntent cancelDeleteSubtask() {
        return simple(Type.CANCEL_DELETE_SUBTASK);
    }
}
