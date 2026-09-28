package com.skyinit.pomodorotimer.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 注册用户档案。表中仅存已注册账户；游客模式不创建本行。
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey
    @NonNull
    public String userId;

    public String nickname;
    public String password;
    public String passwordSalt;
    public String avatarPath;
    public String signature;

    public long createdAt;
    public long lastLoginAt;

    /** 找回登录后须强制重设密码；仅对该用户行有效，禁止跨账户污染。 */
    public boolean mustResetPassword;

    public User() {
        this.userId = "";
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = 0;
        this.mustResetPassword = false;
    }

    @Ignore
    public User(String userId, String nickname, String password) {
        this.userId = userId;
        this.nickname = nickname;
        this.password = password;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = 0;
    }
}
