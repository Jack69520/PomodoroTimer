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

    @Query("SELECT * FROM blocked_apps ORDER BY category, appName")
    LiveData<List<BlockedApp>> getAllBlockedApps();

    @Query("SELECT * FROM blocked_apps WHERE isEnabled = 1 ORDER BY category, appName")
    LiveData<List<BlockedApp>> getEnabledBlockedApps();

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    BlockedApp getBlockedAppByPackage(String packageName);

    @Query("UPDATE blocked_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    void updateAppEnabled(String packageName, boolean enabled);

    @Query("UPDATE blocked_apps SET isWhitelisted = :whitelisted WHERE packageName = :packageName")
    void updateAppWhitelisted(String packageName, boolean whitelisted);

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    void deleteByPackage(String packageName);

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE isEnabled = 1")
    int getEnabledBlockedAppsCount();

    @Query("SELECT * FROM blocked_apps WHERE isWhitelisted = 1")
    List<BlockedApp> getWhitelistedAppsSync();

    @Insert
    void insertAll(List<BlockedApp> apps);

    @Query("SELECT COUNT(*) FROM blocked_apps")
    int getAppCount();

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_apps WHERE packageName = :packageName)")
    boolean appExists(String packageName);

    @Query("UPDATE blocked_apps SET category = :category, categoryManual = :manual WHERE packageName = :packageName")
    void updateCategory(String packageName, String category, boolean manual);

    @Query("UPDATE blocked_apps SET category = :category, categoryManual = 0 WHERE packageName = :packageName AND categoryManual = 0")
    void updateAutoCategory(String packageName, String category);

    @Query("SELECT * FROM blocked_apps")
    List<BlockedApp> getAllAppsSync();
}
