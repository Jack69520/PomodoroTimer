package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 单次番茄专注期间的应用拦截记录，通过 {@link #sessionStartTime} 与会话关联。
 */
@Entity(
        tableName = "session_app_block_records",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("userId"),
                @Index(value = {"userId", "sessionStartTime"})
        }
)
public class SessionAppBlockRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;
    /** 对应 {@link PomodoroSession#startTime}，用于在会话入库前暂存拦截记录。 */
    public long sessionStartTime;
    public int sequenceNumber;
    public String appPackageName;
    public String appName;
    public long blockTimeMillis;
}
