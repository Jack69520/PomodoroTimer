package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "pomodoro_sessions")
public class PomodoroSession {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;
    public long startTime;
    public long endTime;
    public long duration;
    public boolean completed;
    public String pauseReason;
    public int pauseCount;
    public String pauseReasons;
    public boolean earlyEnd;
    public String notes;
    public int taskId;
    /** -1 表示普通待办；>=0 表示待办集子任务 */
    public int subTaskId;
    public String category;
    public String tags;

    public PomodoroSession() {
        this.startTime = System.currentTimeMillis();
        this.completed = false;
        this.pauseCount = 0;
        this.earlyEnd = false;
        this.subTaskId = -1;
    }

    @Ignore
    public PomodoroSession(long startTime, long duration, int taskId, String category) {
        this.startTime = startTime;
        this.duration = duration;
        this.taskId = taskId;
        this.category = category;
        this.completed = false;
        this.pauseCount = 0;
        this.earlyEnd = false;
        this.subTaskId = -1;
    }
}
