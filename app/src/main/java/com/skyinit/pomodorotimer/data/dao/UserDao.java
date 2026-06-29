package com.skyinit.pomodorotimer.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.skyinit.pomodorotimer.data.entity.User;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteUserById(String userId);

    @Query("SELECT * FROM users WHERE userId = :userId")
    User getUserById(String userId);

    @Query("SELECT * FROM users WHERE userId = :userId AND nickname = :nickname")
    User getUserByIdAndNickname(String userId, String nickname);

    @Query("SELECT * FROM users WHERE userId = :userId AND password = :password")
    User login(String userId, String password);

    @Query("SELECT COUNT(*) FROM users WHERE userId = :userId")
    int checkUserIdExists(String userId);

    @Query("UPDATE users SET lastLoginAt = :loginTime WHERE userId = :userId")
    void updateLastLoginTime(String userId, long loginTime);

    @Query("UPDATE users SET nickname = :nickname WHERE userId = :userId")
    void updateNickname(String userId, String nickname);

    @Query("UPDATE users SET password = :password, passwordSalt = :passwordSalt WHERE userId = :userId")
    void updatePassword(String userId, String password, String passwordSalt);

    @Query("UPDATE users SET avatarPath = :avatarPath WHERE userId = :userId")
    void updateAvatar(String userId, String avatarPath);

    @Query("UPDATE users SET signature = :signature WHERE userId = :userId")
    void updateSignature(String userId, String signature);
}
