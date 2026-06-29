package com.skyinit.pomodorotimer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.skyinit.pomodorotimer.data.entity.RecurringTask;

import java.util.List;

@Dao
public interface RecurringTaskDao {
    @Insert
    void insert(RecurringTask task);

    @Update
    void update(RecurringTask task);

    @Delete
    void delete(RecurringTask task);

    @Query("SELECT * FROM recurring_tasks WHERE isActive = 1 ORDER BY nextDueDate ASC")
    LiveData<List<RecurringTask>> getActiveRecurringTasks();

    @Query("SELECT * FROM recurring_tasks WHERE isActive = 1 AND nextDueDate <= :currentTime ORDER BY nextDueDate ASC")
    LiveData<List<RecurringTask>> getDueRecurringTasks(long currentTime);

    @Query("SELECT * FROM recurring_tasks WHERE isActive = 1 AND nextDueDate <= :currentTime ORDER BY nextDueDate ASC")
    List<RecurringTask> getDueRecurringTasksSync(long currentTime);

    @Query("SELECT * FROM recurring_tasks WHERE recurrenceType = :type AND isActive = 1")
    LiveData<List<RecurringTask>> getRecurringTasksByType(int type);

    @Query("UPDATE recurring_tasks SET nextDueDate = :nextDueDate WHERE id = :id")
    void updateNextDueDate(int id, long nextDueDate);
}
