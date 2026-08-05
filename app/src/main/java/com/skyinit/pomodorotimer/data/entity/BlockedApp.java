package com.skyinit.pomodorotimer.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 屏蔽应用实体，按账户隔离存储。
 * {@link #isEnabled} 为 true 时专注期间会被拦截；{@link #isWhitelisted} 优先于 isEnabled。
 * {@link #provenance} / {@link #blockingRole} 为扫描时写入的来源与策略角色快照。
 */
@Entity(
        tableName = "blocked_apps",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("userId"),
                @Index(value = {"userId", "packageName"}, unique = true)
        }
)
public class BlockedApp {
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** 应用屏蔽配置属于账户级数据，账户切换后不得复用其他账户的规则。 */
    public String userId;
    public String packageName;
    public String appName;
    public String category;
    /** 用户是否手动设置过分类；为 true 时复扫不覆盖分类 */
    public boolean categoryManual;
    /** {@link com.skyinit.pomodorotimer.domain.appidentity.AppProvenance} 存储值 */
    public String provenance;
    /** {@link com.skyinit.pomodorotimer.domain.blocking.BlockingRole} 存储值 */
    public String blockingRole;
    public boolean isEnabled;
    public boolean isWhitelisted;
    public long createdTime;

    public BlockedApp() {
        this.createdTime = System.currentTimeMillis();
        this.isEnabled = true;
        this.isWhitelisted = false;
        this.categoryManual = false;
        this.provenance = "THIRD_PARTY";
        this.blockingRole = "DEFAULT_BLOCK";
    }

    @Ignore
    public BlockedApp(String packageName, String appName, String category) {
        this.packageName = packageName;
        this.appName = appName;
        this.category = category;
        this.isEnabled = true;
        this.isWhitelisted = false;
        this.categoryManual = false;
        this.provenance = "THIRD_PARTY";
        this.blockingRole = "DEFAULT_BLOCK";
        this.createdTime = System.currentTimeMillis();
    }
}
