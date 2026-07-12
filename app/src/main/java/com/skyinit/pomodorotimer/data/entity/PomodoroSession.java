package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 已完成的番茄钟会话记录，供统计页与日历页查询展示。
 * <p>
 * {@link #taskId} / {@link #subTaskId} 为历史关联字段，<strong>故意不建立外键</strong>：
 * 删除待办后仍保留计时统计（本项目以计时数据为核心，待办仅为辅助维度）。
 */
@Entity(
        tableName = "pomodoro_sessions",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("userId"),
                @Index(value = {"userId", "startTime"}),
                @Index(value = {"userId", "completed", "startTime"})
        }
)
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
    /** 关联待办 ID；待办删除后保留为历史快照，不做外键级联。 */
    public int taskId;
    /** -1 表示普通待办；>=0 表示待办集子任务（同样不做外键级联）。 */
    public int subTaskId;
    public String category;
    public String tags;
    /** 本次专注期间的应用拦截总次数。 */
    public int blockEventCount;

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
