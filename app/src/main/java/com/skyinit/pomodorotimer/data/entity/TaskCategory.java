package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_categories")
public class TaskCategory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String color;
    public int iconResId;
    public boolean isDefault;

    public TaskCategory() {}

    @Ignore
    public TaskCategory(String name, String color, int iconResId) {
        this.name = name;
        this.color = color;
        this.iconResId = iconResId;
        this.isDefault = false;
    }
}
