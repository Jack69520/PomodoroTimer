package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.R;
import android.content.Context;
import androidx.lifecycle.LiveData;
import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.entity.TodoItem;

import java.util.Calendar;
import java.util.List;

public class TodoFilterManager {
    public enum FilterType {
        ALL,           // 全部
        PRIORITY,      // 按优先级
        DUE_DATE,      // 按截止日期
        CATEGORY,      // 按类别
        PRIORITY_AND_DUE_DATE // 按优先级和截止日期组合
    }

    public enum PriorityFilter {
        ALL(0),
        LOW(0),
        MEDIUM(1),
        HIGH(2),
        URGENT(3);

        private final int value;

        PriorityFilter(int value) {
            this.value = value;
        }

        public int getValue() { return value; }

        public String getDisplayName(Context context) {
            String[] options = context.getResources().getStringArray(R.array.filter_priority_options);
            return options[ordinal()];
        }
    }

    public enum DueDateFilter {
        ALL(0, 0),
        TODAY(0, 1),
        TOMORROW(1, 1),
        THIS_WEEK(0, 7),
        NEXT_WEEK(7, 7),
        THIS_MONTH(0, 30),
        OVERDUE(-365, 0);

        private final int daysFromNow;
        private final int daysRange;

        DueDateFilter(int daysFromNow, int daysRange) {
            this.daysFromNow = daysFromNow;
            this.daysRange = daysRange;
        }

        public String getDisplayName(Context context) {
            String[] options = context.getResources().getStringArray(R.array.filter_due_date_options);
            return options[ordinal()];
        }
        
        public long[] getTimeRange() {
            // 统一以“当天零点”为基准，确保“截至当天不算过期”
            Calendar base = Calendar.getInstance();
            base.set(Calendar.HOUR_OF_DAY, 0);
            base.set(Calendar.MINUTE, 0);
            base.set(Calendar.SECOND, 0);
            base.set(Calendar.MILLISECOND, 0);

            long startTime, endTime;

            if (this == OVERDUE) {
                // 已过期：结束于今天零点（不包含今天）
                startTime = 0;
                endTime = base.getTimeInMillis();
            } else if (this == ALL) {
                startTime = 0;
                endTime = Long.MAX_VALUE;
            } else {
                Calendar startCal = (Calendar) base.clone();
                startCal.add(Calendar.DAY_OF_MONTH, daysFromNow);
                startTime = startCal.getTimeInMillis();

                Calendar endCal = (Calendar) startCal.clone();
                endCal.add(Calendar.DAY_OF_MONTH, daysRange);
                endTime = endCal.getTimeInMillis();
            }

            return new long[]{startTime, endTime};
        }
    }

    private final TodoDao todoDao;
    private final AccountManager accountManager;
    private final Context appContext;

    public TodoFilterManager(Context context) {
        appContext = context.getApplicationContext();
        todoDao = AppDatabase.getDatabase(context).todoDao();
        accountManager = AccountManager.getInstance(context);
    }

    public static String filterAllLabel(Context context) {
        return context.getString(R.string.common_filter_all);
    }

    private boolean isFilterAllCategory(String category) {
        return category == null || category.isEmpty()
                || filterAllLabel(appContext).equals(category);
    }

    /**
     * 获取智能排序的任务列表
     */
    public LiveData<List<TodoItem>> getTodosWithSmartSort(boolean completed) {
        String userId = accountManager.requireActiveUserId();
        // 过期的判断以"今天零点"为界，避免"截至当天"的任务被误判为过期
        Calendar startOfToday = Calendar.getInstance();
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);
        return todoDao.getTodosWithSmartSort(userId, completed, startOfToday.getTimeInMillis());
    }

    /**
     * 按优先级筛选任务
     */
    public LiveData<List<TodoItem>> getTodosByPriority(boolean completed, PriorityFilter priorityFilter) {
        String userId = accountManager.requireActiveUserId();
        
        if (priorityFilter == PriorityFilter.ALL) {
            return todoDao.getTodosByCompletion(userId, completed);
        } else {
            return todoDao.getTodosByPriority(userId, priorityFilter.getValue());
        }
    }

    /**
     * 按截止日期筛选任务
     */
    public LiveData<List<TodoItem>> getTodosByDueDate(boolean completed, DueDateFilter dueDateFilter) {
        String userId = accountManager.requireActiveUserId();
        
        if (dueDateFilter == DueDateFilter.ALL) {
            return todoDao.getTodosByCompletion(userId, completed);
        }
        
        long[] timeRange = dueDateFilter.getTimeRange();
        return todoDao.getTodosByDueDateRange(userId, completed, timeRange[0], timeRange[1]);
    }

    /**
     * 按类别筛选任务
     */
    public LiveData<List<TodoItem>> getTodosByCategory(boolean completed, String category) {
        String userId = accountManager.requireActiveUserId();
        
        if (isFilterAllCategory(category)) {
            return todoDao.getTodosByCompletion(userId, completed);
        } else {
            return todoDao.getTodosByCategory(userId, category);
        }
    }

    /**
     * 按优先级和截止日期组合筛选
     */
    public LiveData<List<TodoItem>> getTodosByPriorityAndDueDate(boolean completed, PriorityFilter priorityFilter, DueDateFilter dueDateFilter) {
        String userId = accountManager.requireActiveUserId();
        
        if (priorityFilter == PriorityFilter.ALL && dueDateFilter == DueDateFilter.ALL) {
            return todoDao.getTodosByCompletion(userId, completed);
        }
        
        if (priorityFilter == PriorityFilter.ALL) {
            return getTodosByDueDate(completed, dueDateFilter);
        }
        
        if (dueDateFilter == DueDateFilter.ALL) {
            return getTodosByPriority(completed, priorityFilter);
        }
        
        long[] timeRange = dueDateFilter.getTimeRange();
        return todoDao.getTodosByPriorityAndDueDate(userId, completed, priorityFilter.getValue(), timeRange[0], timeRange[1]);
    }

    /**
     * 获取所有类别
     */
    public LiveData<List<String>> getAllCategories() {
        String userId = accountManager.requireActiveUserId();
        return todoDao.getAllCategories(userId);
    }

    /**
     * 获取所有标签
     */
    public LiveData<List<String>> getAllTags() {
        String userId = accountManager.requireActiveUserId();
        return todoDao.getAllTags(userId);
    }
}
