package com.skyinit.pomodorotimer.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.UserPomodoroSettingsRepository;
import com.skyinit.pomodorotimer.util.AppExecutors;

/**
 * 设置页 ViewModel。所有番茄偏好写操作均在 diskIo 上：同步读 → 改 → 同步写 → 再 post LiveData，
 * 避免异步 upsert 未完成时 getSettingsSync 把内存缓存打回旧值。
 */
public class SettingsViewModel extends ViewModel {

    private final UserPomodoroSettingsRepository pomodoroSettingsRepository;
    private final AppExecutors executors = AppExecutors.getInstance();

    private final MutableLiveData<UserPomodoroSettings> pomodoroSettings = new MutableLiveData<>();

    public SettingsViewModel(UserPomodoroSettingsRepository pomodoroSettingsRepository,
                             TimerSettingsRepository timerSettingsRepository) {
        this.pomodoroSettingsRepository = pomodoroSettingsRepository;
        loadPomodoroSettings();
    }

    public LiveData<UserPomodoroSettings> getPomodoroSettings() {
        return pomodoroSettings;
    }

    public void loadPomodoroSettings() {
        executors.diskIo(() -> {
            UserPomodoroSettings settings = pomodoroSettingsRepository.getSettingsSync();
            pomodoroSettings.postValue(settings);
        });
    }

    public void setDefaultStudyTimeMs(long millis) {
        long clamped = TimerSettingsRepository.clampStudy(millis);
        updateSettings(settings -> settings.defaultStudyTimeMs = clamped);
    }

    public void setDefaultBreakTimeMs(long millis) {
        long clamped = TimerSettingsRepository.clampBreak(millis);
        updateSettings(settings -> settings.defaultBreakTimeMs = clamped);
    }

    public void setMaxPauseCount(int count) {
        int clamped = TimerSettingsRepository.clampMaxPauseCount(count);
        updateSettings(settings -> settings.maxPauseCount = clamped);
    }

    public void setDndDuringFocusEnabled(boolean enabled) {
        updateSettings(settings -> settings.dndDuringFocusEnabled = enabled);
    }

    public void setAutoBlockDuringPomodoro(boolean enabled) {
        updateSettings(settings -> settings.autoBlockDuringPomodoro = enabled);
    }

    public void setAutoStartAfterBreak(boolean enabled) {
        updateSettings(settings -> settings.autoStartAfterBreak = enabled);
    }

    public void setLongBreakEnabled(boolean enabled) {
        updateSettings(settings -> settings.longBreakEnabled = enabled);
    }

    public void setPomodorosBeforeLongBreak(int count) {
        updateSettings(settings -> settings.pomodorosBeforeLongBreak =
                UserPomodoroSettingsRepository.clampPomodorosBeforeLongBreak(count));
    }

    public void setLongBreakDurationMinutes(int minutes) {
        long clampedMinutes = Math.max(10, Math.min(15, minutes));
        updateSettings(settings -> settings.longBreakDurationMs = clampedMinutes * 60_000L);
    }

    private interface SettingsMutator {
        void mutate(UserPomodoroSettings settings);
    }

    private void updateSettings(SettingsMutator mutator) {
        executors.diskIo(() -> {
            UserPomodoroSettings settings = pomodoroSettingsRepository.getSettingsSync();
            mutator.mutate(settings);
            pomodoroSettingsRepository.updateSettingsSync(settings);
            pomodoroSettings.postValue(UserPomodoroSettingsRepository.copySettings(settings));
        });
    }
}
