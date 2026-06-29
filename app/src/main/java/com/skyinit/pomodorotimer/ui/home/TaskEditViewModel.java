package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.repository.RecurringTaskManager;
import com.skyinit.pomodorotimer.data.repository.TaskRepository;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务编辑页 ViewModel：加载/保存普通待办与待办集。
 */
public class TaskEditViewModel extends AndroidViewModel {

    public static final class SaveResult {
        public final boolean success;
        public final String message;

        SaveResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private final TaskRepository taskRepository;
    private final RecurringTaskManager recurringTaskManager;
    private final int initialTaskId;
    private final int initialTaskType;

    private final MutableLiveData<TodoItem> taskLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<SubTask>> subtasksLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<SaveResult> saveResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    /** 新建待办集时，父任务尚未入库，子任务暂存于此 */
    private final List<SubTask> pendingSubtasks = new ArrayList<>();
    private boolean recurringEnabled;
    private int recurrenceTypeIndex;

    public TaskEditViewModel(@NonNull Application application,
                             TaskRepository taskRepository,
                             RecurringTaskManager recurringTaskManager,
                             int taskId,
                             int taskType) {
        super(application);
        this.taskRepository = taskRepository;
        this.recurringTaskManager = recurringTaskManager;
        this.initialTaskId = taskId;
        this.initialTaskType = taskType;
        loadTask();
    }

    public LiveData<TodoItem> getTask() {
        return taskLiveData;
    }

    public LiveData<List<SubTask>> getSubtasks() {
        return subtasksLiveData;
    }

    public LiveData<SaveResult> getSaveResult() {
        return saveResultLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public boolean isNewTask() {
        return initialTaskId <= 0;
    }

    public boolean isCollection() {
        TodoItem task = taskLiveData.getValue();
        return task != null && task.isCollection();
    }

    public void setRecurringOptions(boolean enabled, int recurrenceTypeIndex) {
        this.recurringEnabled = enabled;
        this.recurrenceTypeIndex = recurrenceTypeIndex;
    }

    private void loadTask() {
        loadingLiveData.setValue(true);
        taskRepository.runOnDisk(() -> {
            TodoItem task;
            if (initialTaskId > 0) {
                task = taskRepository.getTaskByIdSync(initialTaskId);
                if (task == null) {
                    postSaveResult(false, getApplication().getString(R.string.task_error_not_found));
                    loadingLiveData.postValue(false);
                    return;
                }
            } else {
                task = new TodoItem("");
                task.taskType = initialTaskType;
                if (task.isCollection()) {
                    task.estimatedPomodoros = 0;
                    task.completedPomodoros = 0;
                }
            }

            List<SubTask> subtasks;
            if (task.id > 0 && task.isCollection()) {
                subtasks = taskRepository.getSubtasksSync(task.id);
            } else if (task.id <= 0 && task.isCollection()) {
                subtasks = new ArrayList<>(pendingSubtasks);
            } else {
                subtasks = new ArrayList<>();
            }

            taskLiveData.postValue(task);
            subtasksLiveData.postValue(subtasks);
            loadingLiveData.postValue(false);
        });
    }

    public void addSubtask(String title, int estimatedPomodoros) {
        TodoItem task = taskLiveData.getValue();
        if (task == null || !task.isCollection()) {
            return;
        }
        int est = Math.max(1, Math.min(estimatedPomodoros, 99));
        if (task.id > 0) {
            SubTask subTask = new SubTask(task.id, title, subtasksLiveData.getValue().size());
            subTask.estimatedPomodoros = est;
            taskRepository.runOnDisk(() -> {
                taskRepository.insertSubTask(subTask);
                List<SubTask> updated = taskRepository.getSubtasksSync(task.id);
                subtasksLiveData.postValue(updated);
            });
        } else {
            SubTask pending = new SubTask(0, title, pendingSubtasks.size());
            pending.estimatedPomodoros = est;
            pendingSubtasks.add(pending);
            subtasksLiveData.setValue(new ArrayList<>(pendingSubtasks));
        }
    }

    public void updateSubtaskPomodoros(SubTask subTask, int estimatedPomodoros) {
        subTask.estimatedPomodoros = Math.max(1, Math.min(estimatedPomodoros, 99));
        if (subTask.id > 0) {
            taskRepository.runOnDisk(() -> taskRepository.updateSubTask(subTask));
        }
    }

    public void toggleSubtask(SubTask subTask, boolean completed) {
        if (subTask.id > 0) {
            taskRepository.runOnDisk(() -> taskRepository.toggleSubTask(subTask, completed));
            taskRepository.runOnDisk(() -> {
                TodoItem task = taskLiveData.getValue();
                if (task != null && task.id > 0) {
                    subtasksLiveData.postValue(taskRepository.getSubtasksSync(task.id));
                }
            });
        } else {
            subTask.completed = completed;
            subtasksLiveData.setValue(new ArrayList<>(pendingSubtasks));
        }
    }

    public void deleteSubtask(SubTask subTask) {
        if (subTask.id > 0) {
            taskRepository.runOnDisk(() -> {
                taskRepository.deleteSubTask(subTask);
                TodoItem task = taskLiveData.getValue();
                if (task != null) {
                    subtasksLiveData.postValue(taskRepository.getSubtasksSync(task.id));
                }
            });
        } else {
            pendingSubtasks.remove(subTask);
            for (int i = 0; i < pendingSubtasks.size(); i++) {
                pendingSubtasks.get(i).order = i;
            }
            subtasksLiveData.setValue(new ArrayList<>(pendingSubtasks));
        }
    }

    public void save(TodoItem editedTask) {
        if (editedTask.title == null || editedTask.title.trim().isEmpty()) {
            saveResultLiveData.setValue(new SaveResult(false,
                    getApplication().getString(R.string.task_error_title_required)));
            return;
        }
        editedTask.title = editedTask.title.trim();

        if (editedTask.isSimple()) {
            editedTask.estimatedPomodoros = Math.max(1, Math.min(editedTask.estimatedPomodoros, 99));
        } else {
            editedTask.estimatedPomodoros = 0;
            editedTask.completedPomodoros = 0;
        }

        loadingLiveData.setValue(true);
        taskRepository.runOnDisk(() -> {
            try {
                final long parentId;
                if (editedTask.id > 0) {
                    taskRepository.updateTask(editedTask);
                    parentId = editedTask.id;
                } else {
                    parentId = taskRepository.insertTask(editedTask);
                    editedTask.id = (int) parentId;
                }

                if (editedTask.isCollection() && parentId > 0 && !pendingSubtasks.isEmpty()) {
                    for (SubTask pending : pendingSubtasks) {
                        SubTask subTask = new SubTask((int) parentId, pending.title, pending.order);
                        subTask.estimatedPomodoros = pending.estimatedPomodoros;
                        subTask.completed = pending.completed;
                        taskRepository.insertSubTask(subTask);
                    }
                    pendingSubtasks.clear();
                }

                if (recurringEnabled && editedTask.isSimple()) {
                    recurringTaskManager.addRecurringTask(
                            recurringTaskManager.createRuleFromTodo(
                                    editedTask, recurrenceTypeIndex, editedTask.dueDate));
                }

                taskLiveData.postValue(editedTask);
                if (editedTask.isCollection() && parentId > 0) {
                    subtasksLiveData.postValue(taskRepository.getSubtasksSync((int) parentId));
                }
                postSaveResult(true, null);
            } catch (Exception e) {
                postSaveResult(false, getApplication().getString(R.string.task_error_save_failed));
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    private void postSaveResult(boolean success, String message) {
        saveResultLiveData.postValue(new SaveResult(success, message));
    }
}
