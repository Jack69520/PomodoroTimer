package com.skyinit.pomodorotimer.data.entity;

import com.skyinit.pomodorotimer.App;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "blocked_apps")
public class BlockedApp {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String packageName;
    public String appName;
    public String category;
    /** 用户是否手动设置过分类；为 true 时复扫不覆盖分类 */
    public boolean categoryManual;
    public boolean isEnabled;
    public boolean isWhitelisted;
    public long createdTime;

    public BlockedApp() {
        this.createdTime = System.currentTimeMillis();
        this.isEnabled = true;
        this.isWhitelisted = false;
        this.categoryManual = false;
    }

    @Ignore
    public BlockedApp(String packageName, String appName, String category) {
        this.packageName = packageName;
        this.appName = appName;
        this.category = category;
        this.isEnabled = true;
        this.isWhitelisted = false;
        this.categoryManual = false;
        this.createdTime = System.currentTimeMillis();
    }
}
