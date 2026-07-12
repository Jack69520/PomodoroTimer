package com.skyinit.pomodorotimer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.skyinit.pomodorotimer.data.entity.SubTask;

import java.util.List;

@Dao
public interface SubTaskDao {
    @Insert
    long insert(SubTask subTask);

    @Update
    void update(SubTask subTask);

    @Delete
    void delete(SubTask subTask);

    @Query("SELECT * FROM subtasks WHERE parentTaskId = :parentTaskId ORDER BY `order` ASC")
    LiveData<List<SubTask>> getSubtasksByParentId(int parentTaskId);

    @Query("SELECT * FROM subtasks WHERE parentTaskId = :parentTaskId ORDER BY `order` ASC")
    List<SubTask> getSubtasksByParentIdSync(int parentTaskId);

    @Query("SELECT * FROM subtasks WHERE id = :id LIMIT 1")
    SubTask getSubTaskByIdSync(int id);

    @Query("DELETE FROM subtasks WHERE parentTaskId = :parentTaskId")
    void deleteSubtasksByParentId(int parentTaskId);

    @Query("DELETE FROM subtasks WHERE parentTaskId IN (SELECT id FROM todos WHERE userId = :userId)")
    void deleteSubtasksByUserId(String userId);

    @Query("SELECT COUNT(*) FROM subtasks WHERE parentTaskId = :parentTaskId")
    int getSubtaskCount(int parentTaskId);

    @Query("SELECT COUNT(*) FROM subtasks WHERE parentTaskId = :parentTaskId AND completed = 1")
    int getCompletedSubtaskCount(int parentTaskId);

    @Query("UPDATE subtasks SET completedPomodoros = completedPomodoros + 1 WHERE id = :id")
    void incrementCompletedPomodoros(int id);

    @Query("SELECT COALESCE(SUM(estimatedPomodoros), 0) FROM subtasks WHERE parentTaskId = :parentTaskId")
    int getTotalEstimatedPomodoros(int parentTaskId);

    @Query("SELECT COALESCE(SUM(completedPomodoros), 0) FROM subtasks WHERE parentTaskId = :parentTaskId")
    int getTotalCompletedPomodoros(int parentTaskId);
}
