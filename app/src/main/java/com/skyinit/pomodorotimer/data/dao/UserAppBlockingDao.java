package com.skyinit.pomodorotimer.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.skyinit.pomodorotimer.data.entity.UserAppBlocking;

@Dao
public interface UserAppBlockingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserAppBlocking row);

    @Query("SELECT * FROM user_app_blocking WHERE userId = :userId LIMIT 1")
    UserAppBlocking getByUserId(String userId);

    @Query("DELETE FROM user_app_blocking WHERE userId = :userId")
    void deleteByUserId(String userId);
}
