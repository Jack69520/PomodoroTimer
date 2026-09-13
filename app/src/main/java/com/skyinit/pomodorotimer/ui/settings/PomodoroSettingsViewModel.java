package com.skyinit.pomodorotimer.ui.settings;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.UserPomodoroSettingsRepository;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.FocusDndHelper;
import com.skyinit.pomodorotimer.util.LockScreenTimerGate;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;
import com.skyinit.pomodorotimer.util.StudyDurationPickerHelper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 番茄钟设置页 ViewModel：写盘串行在 diskIo，generation 丢弃过期读结果；DND / 锁屏全屏开关防呆。
 */
public class PomodoroSettingsViewModel extends ViewModel {

    private final Application application;
    private final UserPomodoroSettingsRepository pomodoroSettingsRepository;
    private final SettingsManager settingsManager;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<PomodoroSettingsUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<PomodoroSettingsEffect> effects = new SingleLiveEvent<>();

    private final AtomicInteger loadGeneration = new AtomicInteger(0);
    private final AtomicBoolean pendingDndPermission = new AtomicBoolean(false);
    private final AtomicBoolean pendingFsiPermission = new AtomicBoolean(false);
    private final AtomicBoolean pendingNotificationPermission = new AtomicBoolean(false);

    public PomodoroSettingsViewModel(@NonNull Application application,
                                     @NonNull UserPomodoroSettingsRepository pomodoroSettingsRepository,
                                     @NonNull SettingsManager settingsManager) {
        this.application = application;
        this.pomodoroSettingsRepository = pomodoroSettingsRepository;
        this.settingsManager = settingsManager;
        dispatch(PomodoroSettingsIntent.refresh());
    }

    @NonNull
    public LiveData<PomodoroSettingsUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<PomodoroSettingsEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull PomodoroSettingsIntent intent) {
        switch (intent.type) {
            case REFRESH:
                refresh();
                break;
            case SET_STUDY_DURATION:
                updateSettings(s -> s.defaultStudyTimeMs =
                        TimerSettingsRepository.clampStudy(intent.longValue), true);
                break;
            case SET_BREAK_DURATION:
                updateSettings(s -> s.defaultBreakTimeMs =
                        TimerSettingsRepository.clampBreak(intent.longValue), false);
                break;
            case SET_MAX_PAUSE:
                updateSettings(s -> s.maxPauseCount =
                        TimerSettingsRepository.clampMaxPauseCount(intent.intValue), false);
                break;
            case SET_AUTO_START:
                updateSettings(s -> s.autoStartAfterBreak = intent.boolValue, false);
                break;
            case SET_LONG_BREAK_ENABLED:
                updateSettings(s -> s.longBreakEnabled = intent.boolValue, false);
                break;
            case SET_LONG_BREAK_INTERVAL:
                updateSettings(s -> s.pomodorosBeforeLongBreak =
                        UserPomodoroSettingsRepository.clampPomodorosBeforeLongBreak(intent.intValue), false);
                break;
            case SET_LONG_BREAK_DURATION:
                int minutes = Math.max(10, Math.min(15, intent.intValue));
                updateSettings(s -> s.longBreakDurationMs = minutes * 60_000L, false);
                break;
            case SET_DND:
                onSetDnd(intent.boolValue);
                break;
            case SET_AUTO_BLOCK:
                updateSettings(s -> s.autoBlockDuringPomodoro = intent.boolValue, false);
                break;
            case SET_AUTO_DELETE:
                settingsManager.setAutoDeleteEnabled(intent.boolValue);
                refresh();
                break;
            case SET_COLLECTION_PROGRESS_DETAILS:
                settingsManager.setCollectionProgressDetailsEnabled(intent.boolValue);
                refresh();
                break;
            case SET_LOCK_SCREEN_FULLSCREEN:
                onSetLockScreenFullscreen(intent.boolValue);
                break;
            case OPEN_STUDY_DURATION_PICKER: {
                PomodoroSettingsUiState state = uiState.getValue();
                long ms = state != null ? state.studyTimeMs
                        : UserPomodoroSettings.DEFAULT_STUDY_TIME_MS;
                effects.setValue(PomodoroSettingsEffect.showStudyDurationPicker(ms));
                break;
            }
            case OPEN_BREAK_DURATION_PICKER: {
                PomodoroSettingsUiState state = uiState.getValue();
                int idx = state == null ? 4 : clampIndex((int) (state.breakTimeMs / 60_000L) - 1, 30);
                effects.setValue(PomodoroSettingsEffect.showSingleChoice(
                        PomodoroSettingsEffect.ChoiceKind.BREAK_DURATION, idx));
                break;
            }
            case OPEN_PAUSE_COUNT_PICKER: {
                PomodoroSettingsUiState state = uiState.getValue();
                int idx = state == null ? 1 : clampIndex(state.maxPauseCount - 1, 5);
                effects.setValue(PomodoroSettingsEffect.showSingleChoice(
                        PomodoroSettingsEffect.ChoiceKind.PAUSE_COUNT, idx));
                break;
            }
            case OPEN_LONG_BREAK_INTERVAL_PICKER: {
                PomodoroSettingsUiState state = uiState.getValue();
                int idx = state == null ? 2
                        : clampIndex(state.pomodorosBeforeLongBreak
                        - UserPomodoroSettings.MIN_POMODOROS_BEFORE_LONG_BREAK, 7);
                effects.setValue(PomodoroSettingsEffect.showSingleChoice(
                        PomodoroSettingsEffect.ChoiceKind.LONG_BREAK_INTERVAL, idx));
                break;
            }
            case OPEN_LONG_BREAK_DURATION_PICKER: {
                PomodoroSettingsUiState state = uiState.getValue();
                int idx = state == null ? 5 : clampIndex(state.longBreakDurationMinutes - 10, 6);
                effects.setValue(PomodoroSettingsEffect.showSingleChoice(
                        PomodoroSettingsEffect.ChoiceKind.LONG_BREAK_DURATION, idx));
                break;
            }
            case REQUEST_DND_PERMISSION:
                pendingDndPermission.set(true);
                effects.setValue(PomodoroSettingsEffect.openDndSettings());
                break;
            case REQUEST_FULL_SCREEN_INTENT_PERMISSION:
                pendingFsiPermission.set(true);
                effects.setValue(PomodoroSettingsEffect.openFullScreenIntentSettings());
                break;
            case REQUEST_NOTIFICATION_PERMISSION_SETTINGS:
                pendingNotificationPermission.set(true);
                effects.setValue(PomodoroSettingsEffect.openNotificationSettings());
                break;
            case CONFIRM_ENABLE_LOCK_SCREEN_DEGRADED:
                updateSettings(s -> s.lockScreenFullscreenEnabled = true, false);
                break;
            default:
                break;
        }
    }

    /** Activity onResume：同步勿扰 / 锁屏全屏权限与偏好。 */
    @MainThread
    public void onHostResumed() {
        boolean hasAccess = FocusDndHelper.hasPolicyAccess(application);
        if (pendingDndPermission.getAndSet(false)) {
            updateSettings(s -> s.dndDuringFocusEnabled = hasAccess, false);
            return;
        }
        if (pendingFsiPermission.getAndSet(false) || pendingNotificationPermission.getAndSet(false)) {
            LockScreenTimerGate.EnablePrecondition precondition =
                    LockScreenTimerGate.evaluateEnablePreconditions(application);
            if (precondition == LockScreenTimerGate.EnablePrecondition.OK) {
                updateSettings(s -> s.lockScreenFullscreenEnabled = true, false);
                return;
            }
            if (precondition == LockScreenTimerGate.EnablePrecondition.NEED_FULL_SCREEN_INTENT) {
                // 用户从设置返回仍无 FSI：保持关，可再次点开关选择降级
                refresh();
                return;
            }
            refresh();
            return;
        }
        refresh();
    }

    private void onSetDnd(boolean enabled) {
        if (enabled && !FocusDndHelper.hasPolicyAccess(application)) {
            refresh();
            effects.setValue(PomodoroSettingsEffect.showDndPermissionDialog());
            return;
        }
        updateSettings(s -> s.dndDuringFocusEnabled = enabled, false);
    }

    private void onSetLockScreenFullscreen(boolean enabled) {
        if (!enabled) {
            updateSettings(s -> s.lockScreenFullscreenEnabled = false, false);
            return;
        }
        LockScreenTimerGate.EnablePrecondition precondition =
                LockScreenTimerGate.evaluateEnablePreconditions(application);
        if (precondition == LockScreenTimerGate.EnablePrecondition.OK) {
            updateSettings(s -> s.lockScreenFullscreenEnabled = true, false);
            return;
        }
        refresh();
        effects.setValue(PomodoroSettingsEffect.showLockScreenPermissionDialog(precondition));
    }

    private void refresh() {
        int generation = loadGeneration.incrementAndGet();
        executors.diskIo(() -> {
            UserPomodoroSettings settings = pomodoroSettingsRepository.getSettingsSync();
            boolean autoDelete = settingsManager.isAutoDeleteEnabled();
            boolean collectionProgress = settingsManager.isCollectionProgressDetailsEnabled();
            boolean hasAccess = FocusDndHelper.hasPolicyAccess(application);
            if (!hasAccess && settings.dndDuringFocusEnabled) {
                settings.dndDuringFocusEnabled = false;
                pomodoroSettingsRepository.updateSettingsSync(settings);
            }
            // 缺通知权限时强制关闭锁屏全屏（无法安全强拉回）
            if (settings.lockScreenFullscreenEnabled
                    && !LockScreenTimerGate.hasNotificationPermission(application)) {
                settings.lockScreenFullscreenEnabled = false;
                pomodoroSettingsRepository.updateSettingsSync(settings);
            }
            PomodoroSettingsUiState state = toUiState(settings, autoDelete, collectionProgress, hasAccess);
            if (generation != loadGeneration.get()) {
                return;
            }
            mainHandler.post(() -> {
                if (generation != loadGeneration.get()) {
                    return;
                }
                uiState.setValue(state);
            });
        });
    }

    private interface SettingsMutator {
        void mutate(UserPomodoroSettings settings);
    }

    private void updateSettings(SettingsMutator mutator, boolean toastStudyUpdated) {
        int generation = loadGeneration.incrementAndGet();
        executors.diskIo(() -> {
            UserPomodoroSettings settings = pomodoroSettingsRepository.getSettingsSync();
            mutator.mutate(settings);
            pomodoroSettingsRepository.updateSettingsSync(settings);
            boolean autoDelete = settingsManager.isAutoDeleteEnabled();
            boolean collectionProgress = settingsManager.isCollectionProgressDetailsEnabled();
            boolean hasAccess = FocusDndHelper.hasPolicyAccess(application);
            PomodoroSettingsUiState state = toUiState(
                    UserPomodoroSettingsRepository.copySettings(settings),
                    autoDelete,
                    collectionProgress,
                    hasAccess);
            if (generation != loadGeneration.get()) {
                return;
            }
            mainHandler.post(() -> {
                if (generation != loadGeneration.get()) {
                    return;
                }
                uiState.setValue(state);
                if (toastStudyUpdated) {
                    effects.setValue(PomodoroSettingsEffect.showToast(R.string.study_duration_updated));
                }
            });
        });
    }

    @NonNull
    private PomodoroSettingsUiState toUiState(@NonNull UserPomodoroSettings settings,
                                              boolean autoDelete,
                                              boolean collectionProgressDetails,
                                              boolean hasAccess) {
        int breakMin = (int) (settings.defaultBreakTimeMs / 60_000L);
        int longBreakMin = (int) (settings.longBreakDurationMs / 60_000L);
        return new PomodoroSettingsUiState(
                true,
                settings.defaultStudyTimeMs,
                StudyDurationPickerHelper.formatDurationLabel(application, settings.defaultStudyTimeMs),
                settings.defaultBreakTimeMs,
                application.getString(R.string.settings_duration_minutes_format, breakMin),
                settings.maxPauseCount,
                application.getString(R.string.settings_pause_count_format, settings.maxPauseCount),
                settings.autoStartAfterBreak,
                settings.longBreakEnabled,
                settings.pomodorosBeforeLongBreak,
                application.getString(R.string.settings_pomodoro_count_format,
                        settings.pomodorosBeforeLongBreak),
                longBreakMin,
                application.getString(R.string.settings_duration_minutes_format, longBreakMin),
                settings.dndDuringFocusEnabled && hasAccess,
                hasAccess,
                settings.autoBlockDuringPomodoro,
                settings.lockScreenFullscreenEnabled,
                autoDelete,
                collectionProgressDetails
        );
    }

    private static int clampIndex(int index, int size) {
        if (index < 0) {
            return 0;
        }
        if (index >= size) {
            return size - 1;
        }
        return index;
    }
}
