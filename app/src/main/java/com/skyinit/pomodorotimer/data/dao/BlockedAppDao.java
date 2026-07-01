package com.skyinit.pomodorotimer.data.dao;

import com.skyinit.pomodorotimer.App;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;

import java.util.List;

@Dao
public interface BlockedAppDao {
    @Insert
    void insert(BlockedApp blockedApp);

    @Update
    void update(BlockedApp blockedApp);

    @Delete
    void delete(BlockedApp blockedApp);

    @Query("SELECT * FROM blocked_apps WHERE userId = :userId ORDER BY category, appName")
    LiveData<List<BlockedApp>> getAllBlockedApps(String userId);

    @Query("SELECT * FROM blocked_apps WHERE userId = :userId AND isEnabled = 1 ORDER BY category, appName")
    LiveData<List<BlockedApp>> getEnabledBlockedApps(String userId);

    @Query("SELECT * FROM blocked_apps WHERE userId = :userId AND packageName = :packageName")
    BlockedApp getBlockedAppByPackage(String userId, String packageName);

    @Query("UPDATE blocked_apps SET isEnabled = :enabled WHERE userId = :userId AND packageName = :packageName")
    void updateAppEnabled(String userId, String packageName, boolean enabled);

    @Query("UPDATE blocked_apps SET isWhitelisted = :whitelisted WHERE userId = :userId AND packageName = :packageName")
    void updateAppWhitelisted(String userId, String packageName, boolean whitelisted);

    @Query("DELETE FROM blocked_apps WHERE userId = :userId AND packageName = :packageName")
    void deleteByPackage(String userId, String packageName);

    @Query("DELETE FROM blocked_apps WHERE userId = :userId")
    void deleteByUserId(String userId);

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE userId = :userId AND isEnabled = 1")
    int getEnabledBlockedAppsCount(String userId);

    @Query("SELECT * FROM blocked_apps WHERE userId = :userId AND isWhitelisted = 1")
    List<BlockedApp> getWhitelistedAppsSync(String userId);

    @Insert
    void insertAll(List<BlockedApp> apps);

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE userId = :userId")
    int getAppCount(String userId);

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_apps WHERE userId = :userId AND packageName = :packageName)")
    boolean appExists(String userId, String packageName);

    @Query("UPDATE blocked_apps SET category = :category, categoryManual = :manual WHERE userId = :userId AND packageName = :packageName")
    void updateCategory(String userId, String packageName, String category, boolean manual);

    @Query("UPDATE blocked_apps SET category = :category, categoryManual = 0 WHERE userId = :userId AND packageName = :packageName AND categoryManual = 0")
    void updateAutoCategory(String userId, String packageName, String category);

    @Query("SELECT * FROM blocked_apps WHERE userId = :userId")
    List<BlockedApp> getAllAppsSync(String userId);
}
