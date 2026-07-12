package com.skyinit.pomodorotimer.data.repository;

import androidx.lifecycle.LiveData;
import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.AppExecutors;
import java.util.List;

public class TodoRepository {
    private final TodoDao todoDao;
    private final AccountManager accountManager;
    private final TodoFilterManager filterManager;

    public TodoRepository(TodoDao todoDao, AccountManager accountManager, TodoFilterManager filterManager) {
        this.todoDao = todoDao;
        this.accountManager = accountManager;
        this.filterManager = filterManager;
    }

    public LiveData<List<TodoItem>> observeAllTodos() {
        return todoDao.getAllTodos(accountManager.requireActiveUserId());
    }

    public LiveData<List<TodoItem>> observeFilteredTodos(boolean completed,
                                                         TodoFilterManager.PriorityFilter priorityFilter,
                                                         TodoFilterManager.DueDateFilter dueDateFilter,
                                                         String category) {
        LiveData<List<TodoItem>> base = filterManager.getTodosByPriorityAndDueDate(completed, priorityFilter, dueDateFilter);
        if (base == null || category == null
                || (AppCategory.FILTER_ALL != null && AppCategory.FILTER_ALL.equals(category))) {
            return base;
        }
        return filterManager.getTodosByCategory(completed, category);
    }

    public void updateTodo(TodoItem item) {
        AppExecutors.getInstance().diskIo(() -> todoDao.update(item));
    }
}
