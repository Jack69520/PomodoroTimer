package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.skyinit.pomodorotimer.domain.todo.RecurrenceType;
import com.skyinit.pomodorotimer.util.CategoryDefaults;

import java.io.Serializable;

/**
 * 待办实体：普通待办或待办集。
 * <p>
 * {@link #dueDate} 为本地日历日零点，0 表示无日期。
 * {@link #recurrenceType} 见 {@link RecurrenceType}；仅普通待办可非 NONE。
 */
@Entity(
        tableName = "todos",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("userId"),
                @Index(value = {"userId", "completed"}),
                @Index(value = {"userId", "isPinned"}),
                @Index(value = {"userId", "dueDate"})
        }
)
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
    /** 本地零点时间戳；0 = 无截止日期 */
    public long dueDate;
    /** 0=普通待办, 1=待办集 */
    public int taskType;
    public boolean hasSubtasks;
    public int estimatedPomodoros;
    public int completedPomodoros;
    public boolean isPinned;
    public long pinnedTime;
    public long completedTime;
    /** {@link RecurrenceType}；待办集必须为 NONE */
    public int recurrenceType;

    public TodoItem() {
        this.recurrenceType = RecurrenceType.NONE;
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
        this.recurrenceType = RecurrenceType.NONE;
        this.dueDate = 0L;
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
        this.recurrenceType = RecurrenceType.NONE;
        this.dueDate = 0L;
    }

    public boolean isCollection() {
        return taskType == TYPE_COLLECTION;
    }

    public boolean isSimple() {
        return taskType == TYPE_SIMPLE;
    }

    /** 是否启用重复。 */
    public boolean isRecurring() {
        return RecurrenceType.isRecurring(recurrenceType);
    }
}
