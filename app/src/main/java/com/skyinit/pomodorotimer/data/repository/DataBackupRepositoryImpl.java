package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.skyinit.pomodorotimer.data.database.DatabaseMigrations;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link DataBackupRepository} 占位实现：定义备份策略元数据，导出/导入待后续版本实现。
 */
public final class DataBackupRepositoryImpl implements DataBackupRepository {

    private static final String PREFS_POMODORO = "PomodoroPrefs";
    private static final String PREFS_ACCOUNT = "account_prefs";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DataBackupRepositoryImpl(Context context) {
        // context reserved for future export implementation
    }

    @Override
    public boolean isExportSupported() {
        return false;
    }

    @Override
    public void exportUserData(@NonNull String userId, @NonNull File targetDir,
                               @NonNull BackupCallback callback) {
        mainHandler.post(() -> callback.onError(
                BackupError.NOT_IMPLEMENTED,
                "Export will be available in a future release"
        ));
    }

    @Override
    public int getSchemaVersion() {
        return DatabaseMigrations.CURRENT_VERSION;
    }

    @NonNull
    @Override
    public List<String> getDevicePrefNamesForBackup() {
        return Collections.unmodifiableList(Arrays.asList(PREFS_POMODORO, PREFS_ACCOUNT));
    }
}
