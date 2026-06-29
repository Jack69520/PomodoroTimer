package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.skyinit.pomodorotimer.util.CategoryDefaults;

import java.io.Serializable;

@Entity(tableName = "todos")
public class TodoItem implements Serializable {
    public static final int TYPE_SIMPLE = 0;
    public static final int TYPE_COLLECTION = 1;

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String userId;
    public String title;
    public String description;
    public boolean completed;
    public String category;
    public String tags;
    public int priority;
    public long createdTime;
    public long dueDate;
    /** 0=普通待办, 1=待办集 */
    public int taskType;
    public boolean hasSubtasks;
    public int estimatedPomodoros;
    public int completedPomodoros;
    public boolean isPinned;
    public long pinnedTime;
    public long completedTime;

    public TodoItem() {
    }

    @Ignore
    public TodoItem(String title) {
        this.title = title;
        this.completed = false;
        this.createdTime = System.currentTimeMillis();
        this.priority = 1;
        this.taskType = TYPE_SIMPLE;
        this.hasSubtasks = false;
        this.estimatedPomodoros = 1;
        this.completedPomodoros = 0;
        this.category = CategoryDefaults.getOther();
    }

    @Ignore
    public TodoItem(String title, String category, int priority) {
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.completed = false;
        this.createdTime = System.currentTimeMillis();
        this.taskType = TYPE_SIMPLE;
        this.hasSubtasks = false;
        this.estimatedPomodoros = 1;
        this.completedPomodoros = 0;
    }

    public boolean isCollection() {
        return taskType == TYPE_COLLECTION;
    }

    public boolean isSimple() {
        return taskType == TYPE_SIMPLE;
    }
}
