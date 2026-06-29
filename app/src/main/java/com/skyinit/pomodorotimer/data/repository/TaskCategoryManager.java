package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.TaskCategoryDao;
import com.skyinit.pomodorotimer.data.entity.TaskCategory;
import com.skyinit.pomodorotimer.R;
import android.content.Context;
import java.util.List;
import com.skyinit.pomodorotimer.util.AppExecutors;

public class TaskCategoryManager {
    private AppDatabase database;
    private TaskCategoryDao categoryDao;
    private final Context appContext;

    public TaskCategoryManager(Context context) {
        appContext = context.getApplicationContext();
        database = AppDatabase.getDatabase(context);
        categoryDao = database.taskCategoryDao();
        initializeDefaultCategories();
    }

    private void initializeDefaultCategories() {
        AppExecutors.getInstance().diskIo(() -> {
            // 检查是否已有默认分类
            List<TaskCategory> existingCategories = categoryDao.getDefaultCategories().getValue();
            if (existingCategories == null || existingCategories.isEmpty()) {
                String[] categoryNames = appContext.getResources().getStringArray(R.array.task_categories);
                // 创建默认分类
                TaskCategory[] defaultCategories = {
                    new TaskCategory(categoryNames[0], "#FF5722", R.drawable.ic_work),
                    new TaskCategory(categoryNames[1], "#2196F3", R.drawable.ic_study),
                    new TaskCategory(categoryNames[2], "#4CAF50", R.drawable.ic_life),
                    new TaskCategory(categoryNames[3], "#FF9800", R.drawable.ic_sports),
                    new TaskCategory(categoryNames[4], "#9C27B0", R.drawable.ic_entertainment),
                    new TaskCategory(categoryNames[5], "#607D8B", R.drawable.ic_other)
                };

                for (TaskCategory category : defaultCategories) {
                    category.isDefault = true;
                    categoryDao.insert(category);
                }
            }
        });
    }

    public void addCategory(TaskCategory category) {
        AppExecutors.getInstance().diskIo(() -> {
            categoryDao.insert(category);
        });
    }

    public void updateCategory(TaskCategory category) {
        AppExecutors.getInstance().diskIo(() -> {
            categoryDao.update(category);
        });
    }

    public void deleteCategory(TaskCategory category) {
        AppExecutors.getInstance().diskIo(() -> {
            categoryDao.delete(category);
        });
    }
}
