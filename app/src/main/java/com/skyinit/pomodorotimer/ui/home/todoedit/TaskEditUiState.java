package com.skyinit.pomodorotimer.ui.home.todoedit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 任务编辑页不可变快照。
 */
public final class TaskEditUiState {

    @Nullable
    public final TodoItem task;
    @NonNull
    public final List<SubTask> subtasks;
    public final boolean loading;
    public final boolean saving;
    public final boolean isNewTask;
    public final boolean isCollection;

    public TaskEditUiState(@Nullable TodoItem task,
                           @Nullable List<SubTask> subtasks,
                           boolean loading,
                           boolean saving,
                           boolean isNewTask,
                           boolean isCollection) {
        this.task = task;
        this.subtasks = subtasks != null
                ? Collections.unmodifiableList(new ArrayList<>(subtasks))
                : Collections.emptyList();
        this.loading = loading;
        this.saving = saving;
        this.isNewTask = isNewTask;
        this.isCollection = isCollection;
    }

    @NonNull
    public static TaskEditUiState initial(boolean isNewTask, boolean isCollection) {
        return new TaskEditUiState(null, null, true, false, isNewTask, isCollection);
    }

    @NonNull
    public TaskEditUiState copyWith(@Nullable TodoItem task,
                                    @Nullable List<SubTask> subtasks,
                                    @Nullable Boolean loading,
                                    @Nullable Boolean saving) {
        return new TaskEditUiState(
                task != null ? task : this.task,
                subtasks != null ? subtasks : this.subtasks,
                loading != null ? loading : this.loading,
                saving != null ? saving : this.saving,
                this.isNewTask,
                task != null ? task.isCollection() : this.isCollection);
    }
}
