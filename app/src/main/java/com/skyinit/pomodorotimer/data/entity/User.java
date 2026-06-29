package com.skyinit.pomodorotimer.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    public static final int ACCOUNT_TYPE_LOCAL = 0;
    public static final int ACCOUNT_TYPE_REGISTERED = 1;

    @PrimaryKey
    @NonNull
    public String userId;

    public String nickname;
    public String password;
    public String passwordSalt;
    public String avatarPath;
    public String signature;

    /** {@link #ACCOUNT_TYPE_LOCAL} 或 {@link #ACCOUNT_TYPE_REGISTERED} */
    public int accountType;

    public long createdAt;
    public long lastLoginAt;

    public User() {
        this.userId = "";
        this.accountType = ACCOUNT_TYPE_LOCAL;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = 0;
    }

    @Ignore
    public User(String userId, String nickname, String password) {
        this.userId = userId;
        this.nickname = nickname;
        this.password = password;
        this.accountType = ACCOUNT_TYPE_REGISTERED;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = 0;
    }

    public boolean isLocalProfile() {
        return accountType == ACCOUNT_TYPE_LOCAL;
    }

    public boolean isRegistered() {
        return accountType == ACCOUNT_TYPE_REGISTERED;
    }
}
