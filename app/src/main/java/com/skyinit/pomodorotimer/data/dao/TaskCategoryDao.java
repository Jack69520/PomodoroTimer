package com.skyinit.pomodorotimer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.skyinit.pomodorotimer.data.entity.TaskCategory;

import java.util.List;

@Dao
public interface TaskCategoryDao {
    @Insert
    void insert(TaskCategory category);

    @Update
    void update(TaskCategory category);

    @Delete
    void delete(TaskCategory category);

    @Query("SELECT * FROM task_categories ORDER BY isDefault DESC, name ASC")
    LiveData<List<TaskCategory>> getAllCategories();

    @Query("SELECT * FROM task_categories WHERE isDefault = 1")
    LiveData<List<TaskCategory>> getDefaultCategories();

    @Query("SELECT * FROM task_categories WHERE name = :name LIMIT 1")
    TaskCategory getCategoryByName(String name);

    @Query("DELETE FROM task_categories WHERE isDefault = 0")
    void deleteCustomCategories();
}
