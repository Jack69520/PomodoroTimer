package com.skyinit.pomodorotimer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.skyinit.pomodorotimer.data.entity.TodoItem;

import java.util.List;

/**
 * 待办表数据访问。筛选与过期语义由领域层在内存完成，DAO 以全量观察为主。
 */
@Dao
public interface TodoDao {
    @Insert
    long insert(TodoItem todo);

    @Update
    void update(TodoItem todo);

    @Delete
    void delete(TodoItem todo);

    @Query("DELETE FROM todos")
    void deleteAll();

    @Query("DELETE FROM todos WHERE userId = :userId")
    void deleteTodosByUserId(String userId);

    @Query("SELECT * FROM todos WHERE userId = :userId ORDER BY id DESC")
    List<TodoItem> getAllTodosSync(String userId);

    @Query("SELECT * FROM todos WHERE userId = :userId ORDER BY id DESC")
    LiveData<List<TodoItem>> getAllTodos(String userId);

    @Query("SELECT * FROM todos WHERE userId = :userId AND category = :category ORDER BY priority DESC, createdTime DESC")
    LiveData<List<TodoItem>> getTodosByCategory(String userId, String category);

    @Query("SELECT * FROM todos WHERE userId = :userId AND tags LIKE '%' || :tag || '%' ORDER BY priority DESC, createdTime DESC")
    LiveData<List<TodoItem>> getTodosByTag(String userId, String tag);

    @Query("SELECT * FROM todos WHERE userId = :userId AND priority = :priority ORDER BY createdTime DESC")
    LiveData<List<TodoItem>> getTodosByPriority(String userId, int priority);

    @Query("SELECT * FROM todos WHERE userId = :userId AND completed = :completed ORDER BY priority DESC, createdTime DESC")
    LiveData<List<TodoItem>> getTodosByCompletion(String userId, boolean completed);

    @Query("SELECT * FROM todos WHERE userId = :userId AND taskType = 1 ORDER BY priority DESC, createdTime DESC")
    LiveData<List<TodoItem>> getTodoCollections(String userId);

    @Query("SELECT DISTINCT category FROM todos WHERE userId = :userId AND category IS NOT NULL AND category != ''")
    LiveData<List<String>> getAllCategories(String userId);

    @Query("SELECT DISTINCT tags FROM todos WHERE userId = :userId AND tags IS NOT NULL AND tags != ''")
    LiveData<List<String>> getAllTags(String userId);

    @Query("UPDATE todos SET completedPomodoros = :completedPomodoros WHERE id = :id")
    void updateCompletedPomodoros(int id, int completedPomodoros);

    @Query("UPDATE todos SET completedPomodoros = completedPomodoros + 1 WHERE id = :id")
    void incrementCompletedPomodoros(int id);

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    TodoItem getTodoByIdSync(int id);

    @Query("UPDATE todos SET hasSubtasks = :hasSubtasks WHERE id = :id")
    void updateHasSubtasks(int id, boolean hasSubtasks);

    @Query("UPDATE todos SET completedTime = :completedTime WHERE id = :id")
    void updateCompletedTime(int id, long completedTime);

    /** 真正已完成且完成时间早于 cutoff 的任务（供三日清理）。 */
    @Query("SELECT * FROM todos WHERE userId = :userId AND completed = 1 AND completedTime > 0 AND completedTime < :cutoffTime")
    List<TodoItem> getCompletedTodosForDeletion(String userId, long cutoffTime);

    @Query("DELETE FROM todos WHERE userId = :userId AND completed = 1 AND completedTime > 0 AND completedTime < :cutoffTime")
    void deleteExpiredCompletedTodos(String userId, long cutoffTime);

    @Query("SELECT COUNT(*) FROM todos WHERE userId = :userId AND isPinned = 1 AND completed = 0")
    int countActivePinned(String userId);
}
