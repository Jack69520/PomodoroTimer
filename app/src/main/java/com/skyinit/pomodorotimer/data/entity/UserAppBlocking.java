package com.skyinit.pomodorotimer.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 当前注册用户的应用屏蔽开关（列表仍存于 blocked_apps）。
 */
@Entity(
        tableName = "user_app_blocking",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        )
)
public class UserAppBlocking {

    @PrimaryKey
    @NonNull
    public String userId;

    public boolean enabled;

    public UserAppBlocking() {
        this.userId = "";
        this.enabled = false;
    }

    @Ignore
    public UserAppBlocking(@NonNull String userId, boolean enabled) {
        this.userId = userId;
        this.enabled = enabled;
    }
}
