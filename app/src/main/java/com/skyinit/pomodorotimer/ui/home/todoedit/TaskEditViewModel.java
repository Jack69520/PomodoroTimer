package com.skyinit.pomodorotimer.ui.home.todoedit;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.repository.TodoWorkspaceRepository;
import com.skyinit.pomodorotimer.domain.todo.DueDateTime;
import com.skyinit.pomodorotimer.domain.todo.RecurrencePolicy;
import com.skyinit.pomodorotimer.domain.todo.RecurrenceType;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务编辑页 MVI ViewModel：{@link #dispatch(TaskEditIntent)} 为唯一入口。
 * <p>
 * 保存防抖由 {@link #savingInFlight} 保证；加载用 generation 丢弃过期结果。
 */
public class TaskEditViewModel extends AndroidViewModel {

    private final TodoWorkspaceRepository workspace;
    private final int initialTaskId;
    private final int initialTaskType;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<TaskEditUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<TaskEditEffect> effects = new SingleLiveEvent<>();

    private final List<SubTask> pendingSubtasks = new ArrayList<>();
    private final AtomicBoolean savingInFlight = new AtomicBoolean(false);
    private final AtomicInteger loadGeneration = new AtomicInteger(0);

    public TaskEditViewModel(@NonNull Application application,
                             @NonNull TodoWorkspaceRepository workspace,
                             int taskId,
                             int taskType) {
        super(application);
        this.workspace = workspace;
        this.initialTaskId = taskId;
        this.initialTaskType = taskType;
        boolean isCollection = taskType == TodoItem.TYPE_COLLECTION;
        uiState.setValue(TaskEditUiState.initial(taskId <= 0, isCollection));
        dispatch(TaskEditIntent.load());
    }

    @NonNull
    public LiveData<TaskEditUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<TaskEditEffect> getEffects() {
        return effects;
    }

    public boolean isNewTask() {
        return initialTaskId <= 0;
    }

    @MainThread
    public void dispatch(@NonNull TaskEditIntent intent) {
        switch (intent.type) {
            case LOAD:
                loadTask();
                break;
            case SAVE:
                if (intent.draft != null) {
                    save(intent.draft);
                }
                break;
            case ADD_SUBTASK:
                if (intent.subtaskTitle != null) {
                    addSubtask(intent.subtaskTitle, intent.estimatedPomodoros);
                }
                break;
            case TOGGLE_SUBTASK:
                if (intent.subTask != null) {
                    toggleSubtask(intent.subTask, intent.completed);
                }
                break;
            case DELETE_SUBTASK:
                if (intent.subTask != null) {
                    deleteSubtask(intent.subTask);
                }
                break;
            case UPDATE_SUBTASK_POMODOROS:
                if (intent.subTask != null) {
                    updateSubtaskPomodoros(intent.subTask, intent.estimatedPomodoros);
                }
                break;
            default:
                break;
        }
    }

    private void loadTask() {
        final int gen = loadGeneration.incrementAndGet();
        updateState(s -> s.copyWith(null, null, true, null));
        workspace.runOnDisk(() -> {
            TodoItem task;
            if (initialTaskId > 0) {
                task = workspace.getTaskByIdSync(initialTaskId);
                if (task == null) {
                    mainHandler.post(() -> {
                        if (gen != loadGeneration.get()) {
                            return;
                        }
                        effects.setValue(TaskEditEffect.toastRes(R.string.task_error_not_found));
                        updateState(s -> s.copyWith(null, null, false, null));
                    });
                    return;
                }
            } else {
                task = new TodoItem("");
                task.taskType = initialTaskType;
                task.recurrenceType = RecurrenceType.NONE;
                if (task.isCollection()) {
                    task.estimatedPomodoros = 0;
                    task.completedPomodoros = 0;
                }
            }

            List<SubTask> subtasks;
            if (task.id > 0 && task.isCollection()) {
                subtasks = workspace.getSubtasksSync(task.id);
            } else if (task.id <= 0 && task.isCollection()) {
                subtasks = new ArrayList<>(pendingSubtasks);
            } else {
                subtasks = new ArrayList<>();
            }

            final TodoItem loaded = task;
            final List<SubTask> loadedSubs = subtasks;
            mainHandler.post(() -> {
                if (gen != loadGeneration.get()) {
                    return;
                }
                updateState(s -> s.copyWith(loaded, loadedSubs, false, false));
            });
        });
    }

    private void addSubtask(@NonNull String title, int estimatedPomodoros) {
        TaskEditUiState state = requireState();
        TodoItem task = state.task;
        if (task == null || !task.isCollection()) {
            return;
        }
        String trimmed = title.trim();
        if (trimmed.isEmpty()) {
            effects.setValue(TaskEditEffect.toastRes(R.string.task_subtask_hint));
            return;
        }
        int est = Math.max(1, Math.min(estimatedPomodoros, 99));
        if (task.id > 0) {
            int order = state.subtasks.size();
            SubTask subTask = new SubTask(task.id, trimmed, order);
            subTask.estimatedPomodoros = est;
            workspace.runOnDisk(() -> {
                workspace.insertSubTask(subTask);
                List<SubTask> updated = workspace.getSubtasksSync(task.id);
                mainHandler.post(() -> updateState(s -> s.copyWith(null, updated, null, null)));
            });
        } else {
            SubTask pending = new SubTask(0, trimmed, pendingSubtasks.size());
            pending.estimatedPomodoros = est;
            pendingSubtasks.add(pending);
            updateState(s -> s.copyWith(null, new ArrayList<>(pendingSubtasks), null, null));
        }
    }

    private void updateSubtaskPomodoros(@NonNull SubTask subTask, int estimatedPomodoros) {
        subTask.estimatedPomodoros = Math.max(1, Math.min(estimatedPomodoros, 99));
        if (subTask.id > 0) {
            workspace.runOnDisk(() -> workspace.updateSubTask(subTask));
        }
    }

    private void toggleSubtask(@NonNull SubTask subTask, boolean completed) {
        if (subTask.id > 0) {
            workspace.runOnDisk(() -> {
                workspace.toggleSubTask(subTask, completed);
                TodoItem task = requireState().task;
                if (task != null && task.id > 0) {
                    List<SubTask> updated = workspace.getSubtasksSync(task.id);
                    mainHandler.post(() -> updateState(s -> s.copyWith(null, updated, null, null)));
                }
            });
        } else {
            subTask.completed = completed;
            updateState(s -> s.copyWith(null, new ArrayList<>(pendingSubtasks), null, null));
        }
    }

    private void deleteSubtask(@NonNull SubTask subTask) {
        if (subTask.id > 0) {
            workspace.runOnDisk(() -> {
                workspace.deleteSubTask(subTask);
                TodoItem task = requireState().task;
                if (task != null) {
                    List<SubTask> updated = workspace.getSubtasksSync(task.id);
                    mainHandler.post(() -> updateState(s -> s.copyWith(null, updated, null, null)));
                }
            });
        } else {
            pendingSubtasks.remove(subTask);
            for (int i = 0; i < pendingSubtasks.size(); i++) {
                pendingSubtasks.get(i).order = i;
            }
            updateState(s -> s.copyWith(null, new ArrayList<>(pendingSubtasks), null, null));
        }
    }

    private void save(@NonNull TodoItem editedTask) {
        // 防抖：保存进行中忽略重复点击
        if (!savingInFlight.compareAndSet(false, true)) {
            return;
        }
        if (editedTask.title == null || editedTask.title.trim().isEmpty()) {
            savingInFlight.set(false);
            effects.setValue(TaskEditEffect.toastRes(R.string.task_error_title_required));
            return;
        }
        editedTask.title = editedTask.title.trim();
        editedTask.dueDate = DueDateTime.startOfDay(editedTask.dueDate);
        editedTask.recurrenceType = RecurrencePolicy.sanitizeForTask(
                editedTask.recurrenceType, editedTask.isCollection());

        if (editedTask.isSimple()) {
            editedTask.estimatedPomodoros = Math.max(1, Math.min(editedTask.estimatedPomodoros, 99));
        } else {
            editedTask.estimatedPomodoros = 0;
            editedTask.completedPomodoros = 0;
            editedTask.recurrenceType = RecurrenceType.NONE;
        }

        updateState(s -> s.copyWith(null, null, null, true));
        workspace.runOnDisk(() -> {
            try {
                final long parentId;
                if (editedTask.id > 0) {
                    workspace.updateTask(editedTask);
                    parentId = editedTask.id;
                } else {
                    parentId = workspace.insertTask(editedTask);
                    editedTask.id = (int) parentId;
                }

                if (editedTask.isCollection() && parentId > 0 && !pendingSubtasks.isEmpty()) {
                    for (SubTask pending : pendingSubtasks) {
                        SubTask subTask = new SubTask((int) parentId, pending.title, pending.order);
                        subTask.estimatedPomodoros = pending.estimatedPomodoros;
                        subTask.completed = pending.completed;
                        workspace.insertSubTask(subTask);
                    }
                    pendingSubtasks.clear();
                }

                List<SubTask> subs = editedTask.isCollection() && parentId > 0
                        ? workspace.getSubtasksSync((int) parentId)
                        : new ArrayList<>();
                mainHandler.post(() -> {
                    savingInFlight.set(false);
                    updateState(s -> s.copyWith(editedTask, subs, false, false));
                    effects.setValue(TaskEditEffect.saveSuccess());
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    savingInFlight.set(false);
                    updateState(s -> s.copyWith(null, null, false, false));
                    effects.setValue(TaskEditEffect.saveFailed(
                            getApplication().getString(R.string.task_error_save_failed)));
                });
            }
        }, () -> mainHandler.post(() -> {
            savingInFlight.set(false);
            updateState(s -> s.copyWith(null, null, false, false));
            effects.setValue(TaskEditEffect.saveFailed(
                    getApplication().getString(R.string.task_error_save_failed)));
        }));
    }

    @NonNull
    private TaskEditUiState requireState() {
        TaskEditUiState state = uiState.getValue();
        if (state == null) {
            state = TaskEditUiState.initial(initialTaskId <= 0,
                    initialTaskType == TodoItem.TYPE_COLLECTION);
            uiState.setValue(state);
        }
        return state;
    }

    private interface StateTransform {
        TaskEditUiState apply(TaskEditUiState current);
    }

    private void updateState(StateTransform transform) {
        uiState.setValue(transform.apply(requireState()));
    }
}
