package com.skyinit.pomodorotimer.data.dao;

import com.skyinit.pomodorotimer.data.entity.User;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.skyinit.pomodorotimer.ui.statistics.CategoryStats;
import com.skyinit.pomodorotimer.ui.statistics.DailyStats;
import com.skyinit.pomodorotimer.ui.statistics.HourlyStats;
import com.skyinit.pomodorotimer.ui.statistics.PauseReasonStats;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;

import java.util.List;

@Dao
public interface PomodoroSessionDao {
    @Insert
    void insert(PomodoroSession session);

    @Update
    void update(PomodoroSession session);

    @Delete
    void delete(PomodoroSession session);

    @Query("DELETE FROM pomodoro_sessions WHERE userId = :userId")
    void deleteSessionsByUserId(String userId);

    @Query("SELECT * FROM pomodoro_sessions WHERE id = :id LIMIT 1")
    LiveData<PomodoroSession> getSessionById(int id);

    @Query("SELECT * FROM pomodoro_sessions WHERE id = :id LIMIT 1")
    PomodoroSession getSessionByIdSync(int id);

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId ORDER BY startTime DESC")
    LiveData<List<PomodoroSession>> getAllSessions(String userId);

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId AND startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    LiveData<List<PomodoroSession>> getSessionsByDateRange(String userId, long startTime, long endTime);

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 ORDER BY startTime DESC")
    LiveData<List<PomodoroSession>> getCompletedSessions(String userId);

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime")
    LiveData<Integer> getCompletedSessionsCount(String userId, long startTime, long endTime);

    @Query("SELECT SUM(duration) FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime")
    LiveData<Long> getTotalDuration(String userId, long startTime, long endTime);

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId AND startTime >= :startTime AND startTime <= :endTime AND category = :category ORDER BY startTime DESC")
    LiveData<List<PomodoroSession>> getSessionsByCategoryAndDateRange(String userId, long startTime, long endTime, String category);

    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime AND category = :category ORDER BY startTime DESC")
    List<PomodoroSession> getSessionsByCategoryAndDateRangeSync(String userId, long startTime, long endTime, String category);

    @Query("SELECT category, COUNT(*) as count, SUM(duration) as totalDuration FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime GROUP BY category")
    LiveData<List<CategoryStats>> getCategoryStats(String userId, long startTime, long endTime);

    @Query("SELECT category, COUNT(*) as count, SUM(duration) as totalDuration FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime GROUP BY category")
    List<CategoryStats> getCategoryStatsSync(String userId, long startTime, long endTime);

    @Query("SELECT startTime FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime")
    List<Long> getSessionStartTimesInRangeSync(String userId, long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE completed = 1 AND date(startTime/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    int getDailyCompletedCount(long date);

    @Query("SELECT SUM(duration) FROM pomodoro_sessions WHERE completed = 1 AND date(startTime/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    long getDailyTotalDuration(long date);

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE completed = 1 AND startTime >= :startTime AND startTime <= :endTime")
    int getCompletedCountInRange(long startTime, long endTime);

    @Query("SELECT SUM(duration) FROM pomodoro_sessions WHERE completed = 1 AND startTime >= :startTime AND startTime <= :endTime")
    long getTotalDurationInRange(long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime")
    int getCompletedCountInRangeForUser(String userId, long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE userId = :userId AND completed = 1")
    int getTotalCompletedCountForUser(String userId);

    @Query("SELECT SUM(duration) FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime")
    long getTotalDurationInRangeForUser(String userId, long startTime, long endTime);

    @Query("SELECT date(startTime/1000, 'unixepoch') as date, COUNT(*) as count, SUM(duration) as totalDuration FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime GROUP BY date(startTime/1000, 'unixepoch') ORDER BY date")
    LiveData<List<DailyStats>> getDailyStatsInRange(String userId, long startTime, long endTime);

    @Query("SELECT date(startTime/1000, 'unixepoch') as date, COUNT(*) as count, SUM(duration) as totalDuration FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime GROUP BY date(startTime/1000, 'unixepoch') ORDER BY date")
    List<DailyStats> getDailyStatsInRangeSync(String userId, long startTime, long endTime);

    @Query("SELECT pauseReason, COUNT(*) as count FROM pomodoro_sessions WHERE userId = :userId AND pauseReason IS NOT NULL AND pauseReason != '' AND startTime >= :startTime AND startTime <= :endTime GROUP BY pauseReason ORDER BY count DESC")
    List<PauseReasonStats> getPauseReasonStatsSync(String userId, long startTime, long endTime);

    @Query("SELECT CAST(strftime('%H', startTime/1000, 'unixepoch', 'localtime') AS INTEGER) as hour, SUM(duration) as totalDuration FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime GROUP BY CAST(strftime('%H', startTime/1000, 'unixepoch', 'localtime') AS INTEGER) ORDER BY hour")
    LiveData<List<HourlyStats>> getHourlyStatsInRange(String userId, long startTime, long endTime);

    @Query("SELECT CAST(strftime('%H', startTime/1000, 'unixepoch', 'localtime') AS INTEGER) as hour, SUM(duration) as totalDuration FROM pomodoro_sessions WHERE userId = :userId AND completed = 1 AND startTime >= :startTime AND startTime <= :endTime GROUP BY CAST(strftime('%H', startTime/1000, 'unixepoch', 'localtime') AS INTEGER) ORDER BY hour")
    List<HourlyStats> getHourlyStatsInRangeSync(String userId, long startTime, long endTime);
}
