package com.skyinit.pomodorotimer.ui.profile;



import androidx.lifecycle.LiveData;

import androidx.lifecycle.MutableLiveData;

import androidx.lifecycle.ViewModel;



import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;

import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;

import com.skyinit.pomodorotimer.data.repository.UserPomodoroSettingsRepository;

import com.skyinit.pomodorotimer.util.AppExecutors;



public class SettingsViewModel extends ViewModel {



    private final UserPomodoroSettingsRepository pomodoroSettingsRepository;

    private final TimerSettingsRepository timerSettingsRepository;

    private final AppExecutors executors = AppExecutors.getInstance();



    private final MutableLiveData<UserPomodoroSettings> pomodoroSettings = new MutableLiveData<>();



    public SettingsViewModel(UserPomodoroSettingsRepository pomodoroSettingsRepository,

                             TimerSettingsRepository timerSettingsRepository) {

        this.pomodoroSettingsRepository = pomodoroSettingsRepository;

        this.timerSettingsRepository = timerSettingsRepository;

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

        executors.diskIo(() -> {

            timerSettingsRepository.setDefaultStudyTimeMs(millis);

            postLatestSettings();

        });

    }



    public void setDefaultBreakTimeMs(long millis) {

        executors.diskIo(() -> {

            timerSettingsRepository.setDefaultBreakTimeMs(millis);

            postLatestSettings();

        });

    }



    public void setMaxPauseCount(int count) {

        executors.diskIo(() -> {

            timerSettingsRepository.setMaxPauseCount(count);

            postLatestSettings();

        });

    }



    public void setDndDuringFocusEnabled(boolean enabled) {

        executors.diskIo(() -> {

            timerSettingsRepository.setDndDuringFocusEnabled(enabled);

            postLatestSettings();

        });

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



    private void postLatestSettings() {

        UserPomodoroSettings latest = pomodoroSettingsRepository.getSettingsSync();

        pomodoroSettings.postValue(UserPomodoroSettingsRepository.copySettings(latest));

    }



    private interface SettingsMutator {

        void mutate(UserPomodoroSettings settings);

    }



    private void updateSettings(SettingsMutator mutator) {

        executors.diskIo(() -> {

            UserPomodoroSettings settings = pomodoroSettingsRepository.getSettingsSync();

            mutator.mutate(settings);

            pomodoroSettingsRepository.saveSettings(settings);

            pomodoroSettings.postValue(UserPomodoroSettingsRepository.copySettings(settings));

        });

    }

}


