package com.skyinit.pomodorotimer.data.repository;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * 数据备份/导出接口（预留，供后续手动备份与自动云备份实现）。
 * <p>
 * 备份范围策略见 {@code res/xml/backup_rules.xml} 与 {@code data_extraction_rules.xml}。
 */
public interface DataBackupRepository {

    /** 当前版本是否已实现备份导出。 */
    boolean isExportSupported();

    /**
     * 导出指定用户档案的可备份数据到目标目录。
     *
     * @param userId  活跃或历史档案 ID
     * @param targetDir 可写目录（通常为 cache 或用户选择的 Document URI 对应路径）
     * @param callback  结果回调（主线程）
     */
    void exportUserData(@NonNull String userId, @NonNull File targetDir, @NonNull BackupCallback callback);

    /**
     * 从备份包恢复（尚未实现）。
     */
    default void importUserData(@NonNull File backupFile, @NonNull BackupCallback callback) {
        callback.onError(BackupError.NOT_IMPLEMENTED, "Import is not implemented yet");
    }

    /** 返回 Room schema 版本，供备份元数据使用。 */
    int getSchemaVersion();

    /** 返回应纳入备份的 SharedPreferences 名称列表（设备级偏好）。 */
    @NonNull
    List<String> getDevicePrefNamesForBackup();

    interface BackupCallback {
        void onSuccess(@NonNull File outputFile);

        void onError(@NonNull BackupError error, @NonNull String message);
    }

    enum BackupError {
        NOT_IMPLEMENTED,
        IO_ERROR,
        PERMISSION_DENIED,
        INVALID_BACKUP
    }
}
