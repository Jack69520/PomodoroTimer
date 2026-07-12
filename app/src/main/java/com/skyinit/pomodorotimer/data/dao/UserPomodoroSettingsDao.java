package com.skyinit.pomodorotimer.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;

@Dao
public interface UserPomodoroSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserPomodoroSettings settings);

    @Update
    void update(UserPomodoroSettings settings);

    @Query("SELECT * FROM user_pomodoro_settings WHERE userId = :userId LIMIT 1")
    UserPomodoroSettings getByUserId(String userId);

    @Query("DELETE FROM user_pomodoro_settings WHERE userId = :userId")
    void deleteByUserId(String userId);
}
