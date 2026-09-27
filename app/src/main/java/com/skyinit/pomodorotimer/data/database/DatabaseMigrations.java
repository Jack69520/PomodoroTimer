package com.skyinit.pomodorotimer.data.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room 数据库迁移注册表。
 * <p>
 * 递增 {@link #CURRENT_VERSION} 时必须注册对应 {@link Migration}，并更新
 * {@code app/schemas/} 导出文件。禁止使用 destructive fallback。
 * <p>
 * 手动/自动备份接口见 {@link com.skyinit.pomodorotimer.data.repository.DataBackupRepository}。
 */
public final class DatabaseMigrations {

    public static final int CURRENT_VERSION = 2;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "ALTER TABLE pomodoro_sessions ADD COLUMN pausedDurationMs INTEGER NOT NULL DEFAULT 0");
            db.execSQL(
                    "ALTER TABLE pomodoro_sessions ADD COLUMN plannedDurationMs INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration[] ALL = {
            MIGRATION_1_2
    };

    private DatabaseMigrations() {
    }
}
