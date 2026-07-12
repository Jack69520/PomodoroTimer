package com.skyinit.pomodorotimer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.skyinit.pomodorotimer.data.entity.SessionAppBlockRecord;

import java.util.List;

@Dao
public interface SessionAppBlockRecordDao {

    @Insert
    long insert(SessionAppBlockRecord record);

    @Query("SELECT COUNT(*) FROM session_app_block_records WHERE userId = :userId AND sessionStartTime = :sessionStartTime")
    int getCountBySession(String userId, long sessionStartTime);

    @Query("SELECT * FROM session_app_block_records WHERE userId = :userId AND sessionStartTime = :sessionStartTime ORDER BY sequenceNumber ASC")
    LiveData<List<SessionAppBlockRecord>> observeBySession(String userId, long sessionStartTime);

    @Query("SELECT * FROM session_app_block_records WHERE userId = :userId AND sessionStartTime = :sessionStartTime ORDER BY sequenceNumber ASC")
    List<SessionAppBlockRecord> getBySessionSync(String userId, long sessionStartTime);

    @Query("DELETE FROM session_app_block_records WHERE userId = :userId AND sessionStartTime = :sessionStartTime")
    void deleteBySession(String userId, long sessionStartTime);
}
