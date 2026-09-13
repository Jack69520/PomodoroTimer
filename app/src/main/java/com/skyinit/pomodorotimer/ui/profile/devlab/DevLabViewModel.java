package com.skyinit.pomodorotimer.ui.profile.devlab;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.DevLabRepository;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 开发实验室 ViewModel（MVI）：异步采集、自动刷新、筛选与防重入均在此编排。
 */
public class DevLabViewModel extends ViewModel {

    private static final long UPDATE_INTERVAL_MS = 5000L;
    /** filterIndex：0=全部，其后依次 ERROR/WARNING/INFO/DEBUG */
    private static final int FILTER_ALL = 0;

    private final Application application;
    private final DevLabRepository repository;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<DevLabUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<DevLabEffect> effects = new SingleLiveEvent<>();

    private final AtomicInteger storageGeneration = new AtomicInteger();
    private final AtomicInteger memoryGeneration = new AtomicInteger();
    private final AtomicInteger logsGeneration = new AtomicInteger();
    private final AtomicBoolean storageBusy = new AtomicBoolean(false);
    private final AtomicBoolean memoryBusy = new AtomicBoolean(false);
    private final AtomicBoolean logsBusy = new AtomicBoolean(false);

    private final List<DevLabLogEntry> allLogs = new ArrayList<>();
    private int filterIndex = FILTER_ALL;
    private boolean autoUpdateEnabled;
    private boolean pageVisible;
    private boolean bootstrapped;

    @Nullable
    private Runnable autoUpdateRunnable;

    public DevLabViewModel(@NonNull Application application,
                           @NonNull DevLabRepository repository) {
        this.application = application;
        this.repository = repository;
        String placeholder = application.getString(R.string.dev_lab_error_unavailable);
        uiState.setValue(DevLabUiState.empty(placeholder));
    }

    @NonNull
    public LiveData<DevLabUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<DevLabEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull DevLabIntent intent) {
        switch (intent.type) {
            case BOOTSTRAP:
                if (!bootstrapped) {
                    bootstrapped = true;
                    loadStaticInfo();
                    refreshStorage(false);
                    refreshMemory(false);
                    refreshLogs();
                }
                break;
            case REFRESH_STORAGE:
                refreshStorage(true);
                break;
            case REFRESH_MEMORY:
                refreshMemory(true);
                break;
            case REFRESH_RESOURCES:
                refreshStorage(false);
                refreshMemory(false);
                break;
            case REFRESH_LOGS:
                refreshLogs();
                break;
            case REQUEST_CLEAR_LOGS:
                if (allLogs.isEmpty()) {
                    effects.setValue(DevLabEffect.toast(R.string.dev_lab_toast_logs_already_empty));
                } else {
                    effects.setValue(DevLabEffect.showClearConfirm());
                }
                break;
            case CONFIRM_CLEAR_LOGS:
                clearLogs();
                break;
            case SET_AUTO_UPDATE:
                setAutoUpdate(intent.flag);
                break;
            case SET_FILTER_INDEX:
                setFilterIndex(intent.index);
                break;
            case SET_PAGE_VISIBLE:
                setPageVisible(intent.flag);
                break;
            default:
                break;
        }
    }

    private void loadStaticInfo() {
        DevLabRepository.AppInfoSnapshot app = repository.loadAppInfo();
        DevLabRepository.DeviceInfoSnapshot device = repository.loadDeviceInfo();
        DevLabUiState current = requireState();
        publish(current.toBuilder()
                .appInfo(app.appName, app.versionName, app.versionCode, app.error)
                .deviceInfo(device.deviceName, device.deviceModel, device.deviceBrand,
                        device.language, device.androidVersion, device.apiLevel)
                .build());
    }

    private void refreshStorage(boolean userTriggered) {
        if (!storageBusy.compareAndSet(false, true)) {
            return;
        }
        int generation = storageGeneration.incrementAndGet();
        DevLabUiState current = requireState();
        publish(current.toBuilder()
                .storage(current.appStorage, current.totalStorage, current.usedStorage,
                        current.storagePercentText, current.storagePercent, true, current.storageError)
                .build());

        executors.diskIo(() -> {
            DevLabRepository.ResourceSnapshot snapshot = repository.loadStorage();
            mainHandler.post(() -> {
                if (generation != storageGeneration.get()) {
                    storageBusy.set(false);
                    return;
                }
                storageBusy.set(false);
                DevLabUiState state = requireState();
                publish(state.toBuilder()
                        .storage(snapshot.app, snapshot.total, snapshot.used,
                                snapshot.percentText, Math.max(0, snapshot.percent),
                                false, snapshot.error)
                        .build());
                if (userTriggered) {
                    String message = snapshot.error
                            ? application.getString(R.string.dev_lab_log_storage_failed,
                            application.getString(R.string.dev_lab_error_storage_info))
                            : application.getString(R.string.dev_lab_log_storage_updated);
                    appendLocalLog(
                            snapshot.error ? DevLabLogLevel.ERROR : DevLabLogLevel.INFO,
                            application.getString(R.string.dev_lab_log_category_storage),
                            message
                    );
                }
            });
        });
    }

    private void refreshMemory(boolean userTriggered) {
        if (!memoryBusy.compareAndSet(false, true)) {
            return;
        }
        int generation = memoryGeneration.incrementAndGet();
        DevLabUiState current = requireState();
        publish(current.toBuilder()
                .memory(current.appMemory, current.totalMemory, current.usedMemory,
                        current.memoryPercentText, current.memoryPercent, true, current.memoryError)
                .build());

        executors.diskIo(() -> {
            DevLabRepository.ResourceSnapshot snapshot = repository.loadMemory();
            mainHandler.post(() -> {
                if (generation != memoryGeneration.get()) {
                    memoryBusy.set(false);
                    return;
                }
                memoryBusy.set(false);
                DevLabUiState state = requireState();
                publish(state.toBuilder()
                        .memory(snapshot.app, snapshot.total, snapshot.used,
                                snapshot.percentText, Math.max(0, snapshot.percent),
                                false, snapshot.error)
                        .build());
                if (userTriggered) {
                    String message = snapshot.error
                            ? application.getString(R.string.dev_lab_log_memory_failed,
                            application.getString(R.string.dev_lab_error_memory_info))
                            : application.getString(R.string.dev_lab_log_memory_updated);
                    appendLocalLog(
                            snapshot.error ? DevLabLogLevel.ERROR : DevLabLogLevel.INFO,
                            application.getString(R.string.dev_lab_log_category_memory),
                            message
                    );
                }
            });
        });
    }

    private void refreshLogs() {
        if (!logsBusy.compareAndSet(false, true)) {
            return;
        }
        int generation = logsGeneration.incrementAndGet();
        publish(requireState().toBuilder().logsLoading(true).build());

        executors.diskIo(() -> {
            List<DevLabLogEntry> loaded = repository.loadLogcatEntries();
            mainHandler.post(() -> {
                if (generation != logsGeneration.get()) {
                    logsBusy.set(false);
                    return;
                }
                logsBusy.set(false);
                allLogs.clear();
                allLogs.addAll(loaded);
                republishLogs();
            });
        });
    }

    private void clearLogs() {
        logsGeneration.incrementAndGet();
        allLogs.clear();
        republishLogs();
        effects.setValue(DevLabEffect.toast(R.string.dev_lab_toast_logs_cleared));
    }

    private void setAutoUpdate(boolean enabled) {
        if (autoUpdateEnabled == enabled) {
            return;
        }
        autoUpdateEnabled = enabled;
        publish(requireState().toBuilder().autoUpdateEnabled(enabled).build());
        appendLocalLog(
                DevLabLogLevel.INFO,
                application.getString(R.string.dev_lab_log_category_auto_update),
                application.getString(enabled
                        ? R.string.dev_lab_log_auto_update_enabled
                        : R.string.dev_lab_log_auto_update_disabled)
        );
        syncAutoUpdateLoop();
    }

    private void setFilterIndex(int index) {
        int safe = Math.max(0, Math.min(index, DevLabLogLevel.values().length));
        if (filterIndex == safe) {
            return;
        }
        filterIndex = safe;
        republishLogs();
    }

    private void setPageVisible(boolean visible) {
        if (pageVisible == visible) {
            return;
        }
        pageVisible = visible;
        syncAutoUpdateLoop();
    }

    private void syncAutoUpdateLoop() {
        stopAutoUpdate();
        if (!autoUpdateEnabled || !pageVisible) {
            return;
        }
        autoUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!autoUpdateEnabled || !pageVisible) {
                    return;
                }
                refreshStorage(false);
                refreshMemory(false);
                appendLocalLog(
                        DevLabLogLevel.DEBUG,
                        application.getString(R.string.dev_lab_log_category_auto_update),
                        application.getString(R.string.dev_lab_log_auto_refresh)
                );
                mainHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        mainHandler.postDelayed(autoUpdateRunnable, UPDATE_INTERVAL_MS);
    }

    private void stopAutoUpdate() {
        if (autoUpdateRunnable != null) {
            mainHandler.removeCallbacks(autoUpdateRunnable);
            autoUpdateRunnable = null;
        }
    }

    private void appendLocalLog(@NonNull DevLabLogLevel level,
                                @NonNull String tag,
                                @NonNull String message) {
        allLogs.add(repository.createLocalEntry(level, tag, message));
        // 防呆：本地日志上限，避免自动刷新无限膨胀
        int max = 200;
        if (allLogs.size() > max) {
            allLogs.subList(0, allLogs.size() - max).clear();
        }
        republishLogs();
    }

    private void republishLogs() {
        List<DevLabLogEntry> visible = filterLogs(allLogs, filterIndex);
        publish(requireState().toBuilder()
                .filterIndex(filterIndex)
                .logsLoading(false)
                .logs(visible, allLogs.size())
                .build());
    }

    @NonNull
    private static List<DevLabLogEntry> filterLogs(@NonNull List<DevLabLogEntry> source,
                                                   int filterIndex) {
        if (filterIndex == FILTER_ALL) {
            return new ArrayList<>(source);
        }
        int levelOrdinal = filterIndex - 1;
        DevLabLogLevel[] levels = DevLabLogLevel.values();
        if (levelOrdinal < 0 || levelOrdinal >= levels.length) {
            return new ArrayList<>(source);
        }
        DevLabLogLevel target = levels[levelOrdinal];
        List<DevLabLogEntry> filtered = new ArrayList<>();
        for (DevLabLogEntry entry : source) {
            if (entry.level == target) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    @NonNull
    private DevLabUiState requireState() {
        DevLabUiState state = uiState.getValue();
        if (state != null) {
            return state;
        }
        return DevLabUiState.empty(application.getString(R.string.dev_lab_error_unavailable));
    }

    private void publish(@NonNull DevLabUiState state) {
        uiState.setValue(state);
    }

    @Override
    protected void onCleared() {
        stopAutoUpdate();
        storageGeneration.incrementAndGet();
        memoryGeneration.incrementAndGet();
        logsGeneration.incrementAndGet();
        super.onCleared();
    }
}
