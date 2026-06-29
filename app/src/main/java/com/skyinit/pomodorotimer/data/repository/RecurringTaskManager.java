package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.RecurringTaskDao;
import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.entity.RecurringTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import android.content.Context;
import java.util.Calendar;
import java.util.List;
import com.skyinit.pomodorotimer.util.AppExecutors;

public class RecurringTaskManager {
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
            long currentTime = System.currentTimeMillis();
            List<RecurringTask> dueTasks = recurringTaskDao.getDueRecurringTasksSync(currentTime);
            if (dueTasks == null || dueTasks.isEmpty()) {
                return;
            }
            for (RecurringTask task : dueTasks) {
                createTaskFromRecurring(task);
                updateNextDueDate(task);
            }
        });
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
        String userId = recurringTask.userId;
        if (userId == null || userId.isEmpty()) {
            userId = accountManager.requireActiveUserId();
        }

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
