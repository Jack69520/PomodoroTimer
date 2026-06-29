package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "subtasks")
public class SubTask {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int parentTaskId;
    public String title;
    public boolean completed;
    public int order;
    public int estimatedPomodoros;
    public int completedPomodoros;

    public SubTask() {}

    @Ignore
    public SubTask(int parentTaskId, String title, int order) {
        this.parentTaskId = parentTaskId;
        this.title = title;
        this.order = order;
        this.completed = false;
        this.estimatedPomodoros = 1;
        this.completedPomodoros = 0;
    }
}
