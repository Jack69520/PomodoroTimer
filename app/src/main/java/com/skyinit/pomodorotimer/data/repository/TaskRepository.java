package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.SubTaskDao;
import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.model.TodoCollectionSummary;
import com.skyinit.pomodorotimer.util.AppExecutors;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 任务与待办集的数据访问层，供 ViewModel 调用。
 */
public class TaskRepository {

    private final TodoDao todoDao;
    private final SubTaskDao subTaskDao;
    private final AccountManager accountManager;
    private final Executor diskIo;

    public TaskRepository(Context context, AccountManager accountManager) {
        AppDatabase db = AppDatabase.getDatabase(context);
        todoDao = db.todoDao();
        subTaskDao = db.subTaskDao();
        this.accountManager = accountManager;
        diskIo = AppExecutors.getInstance().diskIoExecutor();
    }

    public LiveData<List<SubTask>> observeSubtasks(int parentTaskId) {
        return subTaskDao.getSubtasksByParentId(parentTaskId);
    }

    public TodoItem getTaskByIdSync(int taskId) {
        return todoDao.getTodoByIdSync(taskId);
    }

    public List<SubTask> getSubtasksSync(int parentTaskId) {
        return subTaskDao.getSubtasksByParentIdSync(parentTaskId);
    }

    public SubTask getSubTaskByIdSync(int subTaskId) {
        return subTaskDao.getSubTaskByIdSync(subTaskId);
    }

    public TodoCollectionSummary getCollectionSummarySync(int parentTaskId) {
        int total = subTaskDao.getSubtaskCount(parentTaskId);
        int completed = subTaskDao.getCompletedSubtaskCount(parentTaskId);
        int estPomodoros = subTaskDao.getTotalEstimatedPomodoros(parentTaskId);
        int donePomodoros = subTaskDao.getTotalCompletedPomodoros(parentTaskId);
        return new TodoCollectionSummary(parentTaskId, completed, total, donePomodoros, estPomodoros);
    }

    public Map<Integer, TodoCollectionSummary> loadCollectionSummariesSync(List<TodoItem> todos) {
        Map<Integer, TodoCollectionSummary> map = new HashMap<>();
        if (todos == null) {
            return map;
        }
        for (TodoItem item : todos) {
            if (item != null && item.isCollection()) {
                map.put(item.id, getCollectionSummarySync(item.id));
            }
        }
        return map;
    }

    public void runOnDisk(Runnable runnable) {
        diskIo.execute(runnable);
    }

    public long insertTask(TodoItem task) {
            task.userId = accountManager.requireActiveUserId();
        long id = todoDao.insert(task);
        task.id = (int) id;
        return id;
    }

    public void updateTask(TodoItem task) {
        todoDao.update(task);
    }

    public void deleteTaskWithSubtasks(TodoItem task) {
        subTaskDao.deleteSubtasksByParentId(task.id);
        todoDao.delete(task);
    }

    public long insertSubTask(SubTask subTask) {
        long id = subTaskDao.insert(subTask);
        subTask.id = (int) id;
        todoDao.updateHasSubtasks(subTask.parentTaskId, true);
        return id;
    }

    public void updateSubTask(SubTask subTask) {
        subTaskDao.update(subTask);
        checkParentTaskCompletion(subTask.parentTaskId);
    }

    public void deleteSubTask(SubTask subTask) {
        subTaskDao.delete(subTask);
        int remaining = subTaskDao.getSubtaskCount(subTask.parentTaskId);
        if (remaining == 0) {
            todoDao.updateHasSubtasks(subTask.parentTaskId, false);
        } else {
            checkParentTaskCompletion(subTask.parentTaskId);
        }
    }

    public void toggleSubTask(SubTask subTask, boolean completed) {
        subTask.completed = completed;
        subTaskDao.update(subTask);
        checkParentTaskCompletion(subTask.parentTaskId);
    }

    private void checkParentTaskCompletion(int parentTaskId) {
        int total = subTaskDao.getSubtaskCount(parentTaskId);
        int completedCount = subTaskDao.getCompletedSubtaskCount(parentTaskId);
        TodoItem parent = todoDao.getTodoByIdSync(parentTaskId);
        if (parent == null || !parent.isCollection()) {
            return;
        }
        if (total > 0 && completedCount == total && !parent.completed) {
            parent.completed = true;
            parent.completedTime = System.currentTimeMillis();
            todoDao.update(parent);
        } else if (total > 0 && completedCount < total && parent.completed) {
            parent.completed = false;
            parent.completedTime = 0L;
            todoDao.update(parent);
        }
    }
}
