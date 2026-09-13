package com.skyinit.pomodorotimer.ui.profile.devlab;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * 开发实验室不可变 UI 快照。
 */
public final class DevLabUiState {

    @NonNull
    public final String appName;
    @NonNull
    public final String versionName;
    public final long versionCode;
    public final boolean appInfoError;

    @NonNull
    public final String deviceName;
    @NonNull
    public final String deviceModel;
    @NonNull
    public final String deviceBrand;
    @NonNull
    public final String language;
    @NonNull
    public final String androidVersion;
    @NonNull
    public final String apiLevel;

    @NonNull
    public final String appStorage;
    @NonNull
    public final String totalStorage;
    @NonNull
    public final String usedStorage;
    @NonNull
    public final String storagePercentText;
    public final int storagePercent;
    public final boolean storageLoading;
    public final boolean storageError;

    @NonNull
    public final String appMemory;
    @NonNull
    public final String totalMemory;
    @NonNull
    public final String usedMemory;
    @NonNull
    public final String memoryPercentText;
    public final int memoryPercent;
    public final boolean memoryLoading;
    public final boolean memoryError;

    public final boolean autoUpdateEnabled;
    public final boolean logsLoading;
    public final int filterIndex;
    @NonNull
    public final List<DevLabLogEntry> visibleLogs;
    public final int totalLogCount;

    public DevLabUiState(@NonNull String appName,
                         @NonNull String versionName,
                         long versionCode,
                         boolean appInfoError,
                         @NonNull String deviceName,
                         @NonNull String deviceModel,
                         @NonNull String deviceBrand,
                         @NonNull String language,
                         @NonNull String androidVersion,
                         @NonNull String apiLevel,
                         @NonNull String appStorage,
                         @NonNull String totalStorage,
                         @NonNull String usedStorage,
                         @NonNull String storagePercentText,
                         int storagePercent,
                         boolean storageLoading,
                         boolean storageError,
                         @NonNull String appMemory,
                         @NonNull String totalMemory,
                         @NonNull String usedMemory,
                         @NonNull String memoryPercentText,
                         int memoryPercent,
                         boolean memoryLoading,
                         boolean memoryError,
                         boolean autoUpdateEnabled,
                         boolean logsLoading,
                         int filterIndex,
                         @NonNull List<DevLabLogEntry> visibleLogs,
                         int totalLogCount) {
        this.appName = appName;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.appInfoError = appInfoError;
        this.deviceName = deviceName;
        this.deviceModel = deviceModel;
        this.deviceBrand = deviceBrand;
        this.language = language;
        this.androidVersion = androidVersion;
        this.apiLevel = apiLevel;
        this.appStorage = appStorage;
        this.totalStorage = totalStorage;
        this.usedStorage = usedStorage;
        this.storagePercentText = storagePercentText;
        this.storagePercent = storagePercent;
        this.storageLoading = storageLoading;
        this.storageError = storageError;
        this.appMemory = appMemory;
        this.totalMemory = totalMemory;
        this.usedMemory = usedMemory;
        this.memoryPercentText = memoryPercentText;
        this.memoryPercent = memoryPercent;
        this.memoryLoading = memoryLoading;
        this.memoryError = memoryError;
        this.autoUpdateEnabled = autoUpdateEnabled;
        this.logsLoading = logsLoading;
        this.filterIndex = filterIndex;
        this.visibleLogs = Collections.unmodifiableList(visibleLogs);
        this.totalLogCount = totalLogCount;
    }

    @NonNull
    public static DevLabUiState empty(@NonNull String placeholder) {
        return new DevLabUiState(
                placeholder, placeholder, 0L, false,
                placeholder, placeholder, placeholder, placeholder, placeholder, placeholder,
                placeholder, placeholder, placeholder, placeholder, 0, true, false,
                placeholder, placeholder, placeholder, placeholder, 0, true, false,
                false, true, 0, Collections.emptyList(), 0
        );
    }

    @NonNull
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private String appName;
        private String versionName;
        private long versionCode;
        private boolean appInfoError;
        private String deviceName;
        private String deviceModel;
        private String deviceBrand;
        private String language;
        private String androidVersion;
        private String apiLevel;
        private String appStorage;
        private String totalStorage;
        private String usedStorage;
        private String storagePercentText;
        private int storagePercent;
        private boolean storageLoading;
        private boolean storageError;
        private String appMemory;
        private String totalMemory;
        private String usedMemory;
        private String memoryPercentText;
        private int memoryPercent;
        private boolean memoryLoading;
        private boolean memoryError;
        private boolean autoUpdateEnabled;
        private boolean logsLoading;
        private int filterIndex;
        private List<DevLabLogEntry> visibleLogs;
        private int totalLogCount;

        private Builder(DevLabUiState source) {
            this.appName = source.appName;
            this.versionName = source.versionName;
            this.versionCode = source.versionCode;
            this.appInfoError = source.appInfoError;
            this.deviceName = source.deviceName;
            this.deviceModel = source.deviceModel;
            this.deviceBrand = source.deviceBrand;
            this.language = source.language;
            this.androidVersion = source.androidVersion;
            this.apiLevel = source.apiLevel;
            this.appStorage = source.appStorage;
            this.totalStorage = source.totalStorage;
            this.usedStorage = source.usedStorage;
            this.storagePercentText = source.storagePercentText;
            this.storagePercent = source.storagePercent;
            this.storageLoading = source.storageLoading;
            this.storageError = source.storageError;
            this.appMemory = source.appMemory;
            this.totalMemory = source.totalMemory;
            this.usedMemory = source.usedMemory;
            this.memoryPercentText = source.memoryPercentText;
            this.memoryPercent = source.memoryPercent;
            this.memoryLoading = source.memoryLoading;
            this.memoryError = source.memoryError;
            this.autoUpdateEnabled = source.autoUpdateEnabled;
            this.logsLoading = source.logsLoading;
            this.filterIndex = source.filterIndex;
            this.visibleLogs = source.visibleLogs;
            this.totalLogCount = source.totalLogCount;
        }

        @NonNull
        public Builder appInfo(@NonNull String appName, @NonNull String versionName,
                               long versionCode, boolean error) {
            this.appName = appName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.appInfoError = error;
            return this;
        }

        @NonNull
        public Builder deviceInfo(@NonNull String deviceName, @NonNull String deviceModel,
                                  @NonNull String deviceBrand, @NonNull String language,
                                  @NonNull String androidVersion, @NonNull String apiLevel) {
            this.deviceName = deviceName;
            this.deviceModel = deviceModel;
            this.deviceBrand = deviceBrand;
            this.language = language;
            this.androidVersion = androidVersion;
            this.apiLevel = apiLevel;
            return this;
        }

        @NonNull
        public Builder storage(@NonNull String appStorage, @NonNull String totalStorage,
                               @NonNull String usedStorage, @NonNull String percentText,
                               int percent, boolean loading, boolean error) {
            this.appStorage = appStorage;
            this.totalStorage = totalStorage;
            this.usedStorage = usedStorage;
            this.storagePercentText = percentText;
            this.storagePercent = percent;
            this.storageLoading = loading;
            this.storageError = error;
            return this;
        }

        @NonNull
        public Builder memory(@NonNull String appMemory, @NonNull String totalMemory,
                              @NonNull String usedMemory, @NonNull String percentText,
                              int percent, boolean loading, boolean error) {
            this.appMemory = appMemory;
            this.totalMemory = totalMemory;
            this.usedMemory = usedMemory;
            this.memoryPercentText = percentText;
            this.memoryPercent = percent;
            this.memoryLoading = loading;
            this.memoryError = error;
            return this;
        }

        @NonNull
        public Builder autoUpdateEnabled(boolean enabled) {
            this.autoUpdateEnabled = enabled;
            return this;
        }

        @NonNull
        public Builder logsLoading(boolean loading) {
            this.logsLoading = loading;
            return this;
        }

        @NonNull
        public Builder filterIndex(int index) {
            this.filterIndex = index;
            return this;
        }

        @NonNull
        public Builder logs(@NonNull List<DevLabLogEntry> visibleLogs, int totalLogCount) {
            this.visibleLogs = visibleLogs;
            this.totalLogCount = totalLogCount;
            return this;
        }

        @NonNull
        public DevLabUiState build() {
            return new DevLabUiState(
                    appName, versionName, versionCode, appInfoError,
                    deviceName, deviceModel, deviceBrand, language, androidVersion, apiLevel,
                    appStorage, totalStorage, usedStorage, storagePercentText, storagePercent,
                    storageLoading, storageError,
                    appMemory, totalMemory, usedMemory, memoryPercentText, memoryPercent,
                    memoryLoading, memoryError,
                    autoUpdateEnabled, logsLoading, filterIndex, visibleLogs, totalLogCount
            );
        }
    }
}
