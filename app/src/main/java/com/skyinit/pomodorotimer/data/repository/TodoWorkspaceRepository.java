package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.SubTaskDao;
import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.model.TodoCollectionSummary;
import com.skyinit.pomodorotimer.domain.todo.DueDateTime;
import com.skyinit.pomodorotimer.domain.todo.RecurrencePolicy;
import com.skyinit.pomodorotimer.domain.todo.RecurrenceType;
import com.skyinit.pomodorotimer.domain.todo.TodoCompletionPolicy;
import com.skyinit.pomodorotimer.domain.todo.TodoGrouping;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 待办工作台统一数据门面：列表观察、CRUD、完成推进、改期、子任务、清理。
 * <p>
 * 所有写操作在 diskIo 执行；完成/改期在事务内读改写，避免竞态双推进。
 */
public class TodoWorkspaceRepository {

    private static final String TAG = "TodoWorkspaceRepo";

    private final AppDatabase database;
    private final TodoDao todoDao;
    private final SubTaskDao subTaskDao;
    private final AccountManager accountManager;
    private final SettingsManager settingsManager;
    private final Executor diskIo;

    public TodoWorkspaceRepository(Context context, AccountManager accountManager) {
        this.database = AppDatabase.getDatabase(context);
        this.todoDao = database.todoDao();
        this.subTaskDao = database.subTaskDao();
        this.accountManager = accountManager;
        this.settingsManager = new SettingsManager(context);
        this.diskIo = AppExecutors.getInstance().diskIoExecutor();
    }

    // region 观察

    public LiveData<List<TodoItem>> observeAllTodos() {
        String userId = accountManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            MutableLiveData<List<TodoItem>> empty = new MutableLiveData<>();
            empty.setValue(Collections.emptyList());
            return empty;
        }
        return todoDao.getAllTodos(userId);
    }

    public LiveData<List<SubTask>> observeSubtasks(int parentTaskId) {
        return subTaskDao.getSubtasksByParentId(parentTaskId);
    }

    // endregion

    // region 同步读（须在 diskIo 调用）

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

    // endregion

    public void runOnDisk(Runnable runnable) {
        diskIo.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                AppLog.e(TAG, "Disk operation failed", throwable);
            }
        });
    }

    /** 带失败回调的磁盘任务（供 ViewModel generation 使用）。 */
    public void runOnDisk(Runnable runnable, Runnable onError) {
        diskIo.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                AppLog.e(TAG, "Disk operation failed", throwable);
                if (onError != null) {
                    onError.run();
                }
            }
        });
    }

    // region 写入规范化

    /** 保存前规范化 due / recurrence。 */
    public void normalizeBeforePersist(TodoItem task) {
        if (task == null) {
            return;
        }
        task.dueDate = DueDateTime.startOfDay(task.dueDate);
        task.recurrenceType = RecurrencePolicy.sanitizeForTask(
                task.recurrenceType, task.isCollection());
        // 开启重复但无日期时，默认锚到今天，便于完成推进
        if (RecurrenceType.isRecurring(task.recurrenceType) && task.dueDate <= 0L) {
            task.dueDate = DueDateTime.startOfToday();
        }
    }

    public long insertTask(TodoItem task) {
        normalizeBeforePersist(task);
        task.userId = accountManager.requireActiveUserId();
        if (task.createdTime <= 0L) {
            task.createdTime = System.currentTimeMillis();
        }
        long id = todoDao.insert(task);
        task.id = (int) id;
        return id;
    }

    public void updateTask(TodoItem task) {
        normalizeBeforePersist(task);
        todoDao.update(task);
    }

    /**
     * 删除待办及其子任务。关联番茄会话不删除。
     */
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
        if (subTask == null) {
            return;
        }
        database.runInTransaction(() -> {
            subTaskDao.delete(subTask);
            int remaining = subTaskDao.getSubtaskCount(subTask.parentTaskId);
            if (remaining == 0) {
                todoDao.updateHasSubtasks(subTask.parentTaskId, false);
            } else {
                checkParentTaskCompletion(subTask.parentTaskId);
            }
        });
    }

    /** 按 id 删除子任务（首页滑删）；事务内回写父集完成态。 */
    public void deleteSubTaskByIdSync(int subTaskId) {
        database.runInTransaction(() -> {
            SubTask fresh = subTaskDao.getSubTaskByIdSync(subTaskId);
            if (fresh == null) {
                return;
            }
            subTaskDao.delete(fresh);
            int remaining = subTaskDao.getSubtaskCount(fresh.parentTaskId);
            if (remaining == 0) {
                todoDao.updateHasSubtasks(fresh.parentTaskId, false);
            } else {
                checkParentTaskCompletion(fresh.parentTaskId);
            }
        });
    }

    public void toggleSubTask(SubTask subTask, boolean completed) {
        if (subTask == null) {
            return;
        }
        database.runInTransaction(() -> {
            SubTask fresh = subTaskDao.getSubTaskByIdSync(subTask.id);
            if (fresh == null) {
                return;
            }
            fresh.completed = completed;
            subTaskDao.update(fresh);
            checkParentTaskCompletion(fresh.parentTaskId);
        });
    }

    /**
     * 下一个未完成子任务（按 order 升序）。无则返回 null。
     */
    @androidx.annotation.Nullable
    public SubTask findNextIncompleteSubtaskSync(int parentTaskId) {
        List<SubTask> list = subTaskDao.getSubtasksByParentIdSync(parentTaskId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        SubTask firstIncomplete = null;
        for (SubTask sub : list) {
            if (sub == null) {
                continue;
            }
            if (!sub.completed) {
                if (firstIncomplete == null || sub.order < firstIncomplete.order) {
                    firstIncomplete = sub;
                }
            }
        }
        return firstIncomplete;
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

    // endregion

    // region 完成 / 改期 / 置顶

    /**
     * 事务内完成或取消完成。返回是否发生了重复滚动。
     * <p>
     * 待办集禁止直写完成态：仅由子任务进度经 {@link #checkParentTaskCompletion} 驱动。
     */
    public boolean setCompletedSync(int todoId, boolean completed) {
        final boolean[] rolled = {false};
        database.runInTransaction(() -> {
            TodoItem fresh = todoDao.getTodoByIdSync(todoId);
            if (fresh == null) {
                return;
            }
            // A1：集合完成只跟子任务走，忽略对父卡的直接勾选
            if (fresh.isCollection()) {
                return;
            }
            long startOfToday = DueDateTime.startOfToday();
            if (completed) {
                TodoCompletionPolicy.Result result = TodoCompletionPolicy.complete(
                        fresh, System.currentTimeMillis(), startOfToday);
                rolled[0] = result.rolledToNextOccurrence;
            } else {
                TodoCompletionPolicy.markIncomplete(fresh);
            }
            todoDao.update(fresh);
        });
        return rolled[0];
    }

    /** 快捷改期：写入归一化后的零点日期。 */
    public void rescheduleSync(int todoId, long newDueDate) {
        database.runInTransaction(() -> {
            TodoItem fresh = todoDao.getTodoByIdSync(todoId);
            if (fresh == null) {
                return;
            }
            fresh.dueDate = DueDateTime.startOfDay(newDueDate);
            todoDao.update(fresh);
        });
    }

    /**
     * 置顶切换。若达到上限且试图新置顶则返回 false。
     */
    public boolean togglePinSync(int todoId) {
        final boolean[] ok = {true};
        database.runInTransaction(() -> {
            TodoItem fresh = todoDao.getTodoByIdSync(todoId);
            if (fresh == null) {
                ok[0] = false;
                return;
            }
            if (fresh.isPinned) {
                fresh.isPinned = false;
                fresh.pinnedTime = 0L;
                todoDao.update(fresh);
                return;
            }
            String userId = accountManager.requireActiveUserId();
            int pinned = todoDao.countActivePinned(userId);
            if (pinned >= TodoGrouping.maxPinned()) {
                ok[0] = false;
                return;
            }
            fresh.isPinned = true;
            fresh.pinnedTime = System.currentTimeMillis();
            todoDao.update(fresh);
        });
        return ok[0];
    }

    // endregion

    // region 三日清理

    private static final long AUTO_DELETE_MS = 3L * 24 * 60 * 60 * 1000;

    public void performCleanup() {
        runOnDisk(() -> {
            if (!settingsManager.isAutoDeleteEnabled()) {
                return;
            }
            String userId = accountManager.getCurrentUserId();
            if (userId == null || userId.isEmpty()) {
                return;
            }
            long cutoff = System.currentTimeMillis() - AUTO_DELETE_MS;
            todoDao.deleteExpiredCompletedTodos(userId, cutoff);
        });
    }

    // endregion
}
