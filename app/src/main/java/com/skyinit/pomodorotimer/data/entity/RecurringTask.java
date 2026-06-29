package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "recurring_tasks")
public class RecurringTask {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public int recurrenceType;
    public String recurrencePattern;
    public boolean isActive;
    public long nextDueDate;
    public String category;
    public String tags;
    public String userId;

    public RecurringTask() {}

    @Ignore
    public RecurringTask(String title, int recurrenceType, String recurrencePattern) {
        this.title = title;
        this.recurrenceType = recurrenceType;
        this.recurrencePattern = recurrencePattern;
        this.isActive = true;
        this.nextDueDate = System.currentTimeMillis();
    }
}
