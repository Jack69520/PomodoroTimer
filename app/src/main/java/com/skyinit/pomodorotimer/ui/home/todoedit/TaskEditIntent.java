package com.skyinit.pomodorotimer.ui.home.todoedit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;

/**
 * 任务编辑页用户意图。
 */
public final class TaskEditIntent {

    public enum Type {
        LOAD,
        SAVE,
        ADD_SUBTASK,
        TOGGLE_SUBTASK,
        DELETE_SUBTASK,
        UPDATE_SUBTASK_POMODOROS
    }

    public final Type type;
    @Nullable
    public final TodoItem draft;
    @Nullable
    public final String subtaskTitle;
    public final int estimatedPomodoros;
    @Nullable
    public final SubTask subTask;
    public final boolean completed;

    private TaskEditIntent(Type type,
                           @Nullable TodoItem draft,
                           @Nullable String subtaskTitle,
                           int estimatedPomodoros,
                           @Nullable SubTask subTask,
                           boolean completed) {
        this.type = type;
        this.draft = draft;
        this.subtaskTitle = subtaskTitle;
        this.estimatedPomodoros = estimatedPomodoros;
        this.subTask = subTask;
        this.completed = completed;
    }

    @NonNull
    public static TaskEditIntent load() {
        return new TaskEditIntent(Type.LOAD, null, null, 0, null, false);
    }

    @NonNull
    public static TaskEditIntent save(@NonNull TodoItem draft) {
        return new TaskEditIntent(Type.SAVE, draft, null, 0, null, false);
    }

    @NonNull
    public static TaskEditIntent addSubtask(@NonNull String title, int estimatedPomodoros) {
        return new TaskEditIntent(Type.ADD_SUBTASK, null, title, estimatedPomodoros, null, false);
    }

    @NonNull
    public static TaskEditIntent toggleSubtask(@NonNull SubTask subTask, boolean completed) {
        return new TaskEditIntent(Type.TOGGLE_SUBTASK, null, null, 0, subTask, completed);
    }

    @NonNull
    public static TaskEditIntent deleteSubtask(@NonNull SubTask subTask) {
        return new TaskEditIntent(Type.DELETE_SUBTASK, null, null, 0, subTask, false);
    }

    @NonNull
    public static TaskEditIntent updateSubtaskPomodoros(@NonNull SubTask subTask, int estimated) {
        return new TaskEditIntent(Type.UPDATE_SUBTASK_POMODOROS, null, null, estimated, subTask, false);
    }
}
