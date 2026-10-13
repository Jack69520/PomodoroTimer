package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 番茄钟会话记录，供统计页与日历页查询展示。
 * <p>
 * {@link #duration} 为<strong>有效专注毫秒</strong>（不含暂停）；{@link #endTime} 为真实墙钟结束。
 * {@link #pausedDurationMs} / {@link #plannedDurationMs} 自 DB v2 起写入；历史行为 0（未知）。
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
    /**
     * 是否计入专注时长统计（与 {@link #completed} 拆账：完成数仍看 completed）。
     * 严格模式下超时留痕可为 false。
     */
    public boolean countedInFocusDuration;
    public String notes;
    /** 关联待办 ID；待办删除后保留为历史快照，不做外键级联。 */
    public int taskId;
    /** -1 表示普通待办；>=0 表示待办集子任务（同样不做外键级联）。 */
    public int subTaskId;
    public String category;
    public String tags;
    /** 本次专注期间的应用拦截总次数。 */
    public int blockEventCount;
    /** 累计暂停毫秒（elapsed 段累加）；历史迁移行可能为 0（未知）。 */
    public long pausedDurationMs;
    /** 开局计划学习时长快照；历史迁移行可能为 0（未知）。 */
    public long plannedDurationMs;

    public PomodoroSession() {
        this.startTime = System.currentTimeMillis();
        this.completed = false;
        this.pauseCount = 0;
        this.earlyEnd = false;
        this.countedInFocusDuration = false;
        this.subTaskId = -1;
        this.pausedDurationMs = 0L;
        this.plannedDurationMs = 0L;
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
        this.countedInFocusDuration = false;
        this.subTaskId = -1;
    }
}
