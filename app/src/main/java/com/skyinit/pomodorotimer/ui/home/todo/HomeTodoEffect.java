package com.skyinit.pomodorotimer.ui.home.todo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;

/**
 * 首页待办一次性副作用。
 */
public final class HomeTodoEffect {

    public enum Type {
        NAVIGATE_EDIT,
        CONFIRM_DELETE,
        CONFIRM_DELETE_SUBTASK,
        SHOW_RESCHEDULE_SHEET,
        TOAST_RES,
        TOAST_TEXT,
        START_TIMER,
        START_SUBTASK_TIMER
    }

    public final Type type;
    public final int taskId;
    public final int taskType;
    public final int todoId;
    public final int subtaskId;
    @StringRes
    public final int resId;
    @Nullable
    public final String text;
    @Nullable
    public final TodoItem todo;
    @Nullable
    public final SubTask subTask;

    private HomeTodoEffect(Type type,
                           int taskId,
                           int taskType,
                           int todoId,
                           int subtaskId,
                           @StringRes int resId,
                           @Nullable String text,
                           @Nullable TodoItem todo,
                           @Nullable SubTask subTask) {
        this.type = type;
        this.taskId = taskId;
        this.taskType = taskType;
        this.todoId = todoId;
        this.subtaskId = subtaskId;
        this.resId = resId;
        this.text = text;
        this.todo = todo;
        this.subTask = subTask;
    }

    @NonNull
    public static HomeTodoEffect navigateEdit(int taskId, int taskType) {
        return new HomeTodoEffect(Type.NAVIGATE_EDIT, taskId, taskType, 0, 0, 0, null, null, null);
    }

    @NonNull
    public static HomeTodoEffect confirmDelete(@NonNull TodoItem todo) {
        return new HomeTodoEffect(Type.CONFIRM_DELETE, todo.id, todo.taskType, todo.id, 0,
                0, null, todo, null);
    }

    @NonNull
    public static HomeTodoEffect confirmDeleteSubtask(@NonNull SubTask subTask) {
        return new HomeTodoEffect(Type.CONFIRM_DELETE_SUBTASK, 0, 0, subTask.parentTaskId, subTask.id,
                0, null, null, subTask);
    }

    @NonNull
    public static HomeTodoEffect showRescheduleSheet(int todoId) {
        return new HomeTodoEffect(Type.SHOW_RESCHEDULE_SHEET, 0, 0, todoId, 0, 0, null, null, null);
    }

    @NonNull
    public static HomeTodoEffect toastRes(@StringRes int resId) {
        return new HomeTodoEffect(Type.TOAST_RES, 0, 0, 0, 0, resId, null, null, null);
    }

    @NonNull
    public static HomeTodoEffect toastText(@NonNull String text) {
        return new HomeTodoEffect(Type.TOAST_TEXT, 0, 0, 0, 0, 0, text, null, null);
    }

    @NonNull
    public static HomeTodoEffect startTimer(@NonNull TodoItem todo) {
        return new HomeTodoEffect(Type.START_TIMER, todo.id, todo.taskType, todo.id, 0,
                0, null, todo, null);
    }

    @NonNull
    public static HomeTodoEffect startSubtaskTimer(@NonNull TodoItem parent, @NonNull SubTask subTask) {
        return new HomeTodoEffect(Type.START_SUBTASK_TIMER, parent.id, parent.taskType, parent.id,
                subTask.id, 0, null, parent, subTask);
    }
}
