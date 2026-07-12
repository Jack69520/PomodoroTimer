package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.RecurringTaskDao;
import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.entity.RecurringTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import android.content.Context;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

public class RecurringTaskManager {
    private static final String TAG = "RecurringTaskManager";
    private static final long PROCESS_DUE_TASKS_TIMEOUT_SECONDS = 30L;

    private final AppDatabase database;
    private final RecurringTaskDao recurringTaskDao;
    private final TodoDao todoDao;
    private final AccountManager accountManager;

    public RecurringTaskManager(Context context) {
        database = AppDatabase.getDatabase(context);
        recurringTaskDao = database.recurringTaskDao();
        todoDao = database.todoDao();
        accountManager = AccountManager.getInstance(context);
    }

    public void addRecurringTask(RecurringTask task) {
        AppExecutors.getInstance().diskIo(() -> recurringTaskDao.insert(task));
    }

    public void updateRecurringTask(RecurringTask task) {
        AppExecutors.getInstance().diskIo(() -> recurringTaskDao.update(task));
    }

    public void deleteRecurringTask(RecurringTask task) {
        AppExecutors.getInstance().diskIo(() -> recurringTaskDao.delete(task));
    }

    public void processDueTasks() {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                processDueTasksOnDisk();
            } catch (Throwable throwable) {
                AppLog.e(TAG, "Async processDueTasks failed", throwable);
            }
        });
    }

    /**
     * 同步处理到期重复待办，供 WorkManager Worker 调用；阻塞直至磁盘 IO 完成并向上抛出失败。
     */
    public void processDueTasksSync() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AppExecutors.getInstance().diskIo(() -> {
            try {
                processDueTasksOnDisk();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        }, throwable -> {
            failure.compareAndSet(null, throwable);
            latch.countDown();
        });
        try {
            if (!latch.await(PROCESS_DUE_TASKS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("processDueTasks timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("processDueTasks interrupted", e);
        }
        Throwable throwable = failure.get();
        if (throwable != null) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new IllegalStateException("processDueTasks failed", throwable);
        }
    }

    private void processDueTasksOnDisk() {
        long currentTime = System.currentTimeMillis();
        List<RecurringTask> dueTasks = recurringTaskDao.getDueRecurringTasksSync(currentTime);
        if (dueTasks == null || dueTasks.isEmpty()) {
            return;
        }
        for (RecurringTask task : dueTasks) {
            database.runInTransaction(() -> {
                createTaskFromRecurring(task);
                updateNextDueDate(task);
            });
        }
    }

    public RecurringTask createRuleFromTodo(TodoItem todo, int recurrenceType, long firstDueDate) {
        RecurringTask recurringTask = new RecurringTask();
        recurringTask.title = todo.title;
        recurringTask.description = todo.description;
        recurringTask.category = todo.category;
        recurringTask.tags = todo.tags;
        recurringTask.recurrenceType = recurrenceType;
        recurringTask.recurrencePattern = String.valueOf(recurrenceType);
        recurringTask.isActive = true;
        recurringTask.userId = todo.userId;
        recurringTask.nextDueDate = firstDueDate > 0 ? firstDueDate : computeInitialDueDate(recurrenceType);
        return recurringTask;
    }

    private long computeInitialDueDate(int recurrenceType) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        switch (recurrenceType) {
            case 1:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case 2:
                calendar.add(Calendar.MONTH, 1);
                break;
            case 0:
            default:
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
        }
        return calendar.getTimeInMillis();
    }

    private void createTaskFromRecurring(RecurringTask recurringTask) {
        String userId = resolveUserId(recurringTask);

        TodoItem task = new TodoItem(recurringTask.title);
        task.userId = userId;
        task.description = recurringTask.description;
        task.category = recurringTask.category;
        task.tags = recurringTask.tags;
        task.dueDate = recurringTask.nextDueDate;
        task.estimatedPomodoros = 1;
        task.completedPomodoros = 0;
        todoDao.insert(task);
    }

    private String resolveUserId(RecurringTask recurringTask) {
        if (recurringTask.userId != null && !recurringTask.userId.isEmpty()) {
            return recurringTask.userId;
        }
        return accountManager.requireActiveUserId();
    }

    private void updateNextDueDate(RecurringTask task) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(task.nextDueDate);

        switch (task.recurrenceType) {
            case 1:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case 2:
                calendar.add(Calendar.MONTH, 1);
                break;
            case 0:
            default:
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
        }

        recurringTaskDao.updateNextDueDate(task.id, calendar.getTimeInMillis());
    }

    public void toggleRecurringTask(RecurringTask task) {
        AppExecutors.getInstance().diskIo(() -> {
            task.isActive = !task.isActive;
            recurringTaskDao.update(task);
        });
    }
}
