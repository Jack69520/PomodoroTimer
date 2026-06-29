package com.skyinit.pomodorotimer.data.database;

import androidx.room.migration.Migration;

/**
 * Room 数据库迁移注册表。
 * <p>
 * 当前为 v1 初始版本；后续 schema 变更时递增 {@link #CURRENT_VERSION} 并在此注册
 * {@link Migration}，同时更新 {@code app/schemas/} 导出文件。
 * <p>
 * 手动/自动备份接口见 {@link com.skyinit.pomodorotimer.data.repository.DataBackupRepository}。
 */
public final class DatabaseMigrations {

    public static final int CURRENT_VERSION = 1;

    public static final Migration[] ALL = {
    };

    private DatabaseMigrations() {
    }
}
