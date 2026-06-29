package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.SubTaskDao;
import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import android.content.Context;
import java.util.List;
import com.skyinit.pomodorotimer.util.AppExecutors;

public class SubTaskManager {
    private final AppDatabase database;
    private final SubTaskDao subTaskDao;
    private final TodoDao todoDao;

    public SubTaskManager(Context context) {
        database = AppDatabase.getDatabase(context);
        subTaskDao = database.subTaskDao();
        todoDao = database.todoDao();
    }

    public List<SubTask> getSubtasksSync(int parentTaskId) {
        return subTaskDao.getSubtasksByParentIdSync(parentTaskId);
    }

    public void addSubTask(int parentTaskId, String title) {
        AppExecutors.getInstance().diskIo(() -> {
            int order = subTaskDao.getSubtaskCount(parentTaskId);
            SubTask subTask = new SubTask(parentTaskId, title, order);
            subTaskDao.insert(subTask);
            todoDao.updateHasSubtasks(parentTaskId, true);
        });
    }

    public void updateSubTask(SubTask subTask) {
        AppExecutors.getInstance().diskIo(() -> subTaskDao.update(subTask));
    }

    public void deleteSubTask(SubTask subTask) {
        AppExecutors.getInstance().diskIo(() -> {
            subTaskDao.delete(subTask);
            int remainingCount = subTaskDao.getSubtaskCount(subTask.parentTaskId);
            if (remainingCount == 0) {
                todoDao.updateHasSubtasks(subTask.parentTaskId, false);
            } else {
                checkParentTaskCompletion(subTask.parentTaskId);
            }
        });
    }

    public void toggleSubTask(SubTask subTask, boolean completed) {
        AppExecutors.getInstance().diskIo(() -> {
            subTask.completed = completed;
            subTaskDao.update(subTask);
            checkParentTaskCompletion(subTask.parentTaskId);
        });
    }

    private void checkParentTaskCompletion(int parentTaskId) {
        int totalSubtasks = subTaskDao.getSubtaskCount(parentTaskId);
        int completedSubtasks = subTaskDao.getCompletedSubtaskCount(parentTaskId);

        if (totalSubtasks > 0 && completedSubtasks == totalSubtasks) {
            TodoItem parentTask = todoDao.getTodoByIdSync(parentTaskId);
            if (parentTask != null && !parentTask.completed) {
                parentTask.completed = true;
                parentTask.completedTime = System.currentTimeMillis();
                todoDao.update(parentTask);
            }
        } else if (totalSubtasks > 0 && completedSubtasks < totalSubtasks) {
            TodoItem parentTask = todoDao.getTodoByIdSync(parentTaskId);
            if (parentTask != null && parentTask.completed) {
                parentTask.completed = false;
                parentTask.completedTime = 0L;
                todoDao.update(parentTask);
            }
        }
    }
}
