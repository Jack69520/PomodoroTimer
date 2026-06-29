package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.entity.User;
import android.content.Context;
import com.skyinit.pomodorotimer.util.AppLog;

import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.entity.TodoItem;

import java.util.List;
import com.skyinit.pomodorotimer.util.AppExecutors;

public class TodoCleanupManager {
    private static final String TAG = "TodoCleanupManager";
    private static final long AUTO_DELETE_DAYS = 3 * 24 * 60 * 60 * 1000L;

    private final TodoDao todoDao;
    private final AccountManager accountManager;
    private final SettingsManager settingsManager;

    public TodoCleanupManager(Context context) {
        todoDao = AppDatabase.getDatabase(context).todoDao();
        accountManager = AccountManager.getInstance(context);
        settingsManager = new SettingsManager(context);
    }

    public void performCleanup() {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                if (!settingsManager.isAutoDeleteEnabled()) return;
                String userId = accountManager.requireActiveUserId();
                long cutoffTime = System.currentTimeMillis() - AUTO_DELETE_DAYS;
                List<TodoItem> todosToDelete = todoDao.getCompletedTodosForDeletion(userId, cutoffTime);
                if (!todosToDelete.isEmpty()) {
                    todoDao.deleteExpiredCompletedTodos(userId, cutoffTime);
                }
            } catch (Exception e) {
                AppLog.e(TAG, "Error during cleanup", e);
            }
        });
    }

    public void markTodoAsCompleted(TodoItem todo) {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                todo.completed = true;
                todo.completedTime = System.currentTimeMillis();
                todoDao.update(todo);
            } catch (Exception e) {
                AppLog.e(TAG, "Error marking todo as completed", e);
            }
        });
    }

    public void markTodoAsIncomplete(TodoItem todo) {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                todo.completed = false;
                todo.completedTime = 0;
                todoDao.update(todo);
            } catch (Exception e) {
                AppLog.e(TAG, "Error marking todo as incomplete", e);
            }
        });
    }

    public void getExpiringCompletedTodosCount(OnCountResultListener listener) {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                String userId = accountManager.requireActiveUserId();
                long cutoffTime = System.currentTimeMillis() - AUTO_DELETE_DAYS;
                List<TodoItem> expiringTodos = todoDao.getCompletedTodosForDeletion(userId, cutoffTime);
                listener.onCountResult(expiringTodos.size());
            } catch (Exception e) {
                AppLog.e(TAG, "Error getting expiring todos count", e);
                listener.onCountResult(0);
            }
        });
    }

    public interface OnCountResultListener { void onCountResult(int count); }
}
