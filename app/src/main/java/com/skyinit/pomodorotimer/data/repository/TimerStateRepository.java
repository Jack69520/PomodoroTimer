package com.skyinit.pomodorotimer.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.service.TimerService;

/**
 * 计时器运行态的唯一数据源（Service → Repository → ViewModel → UI）。
 */
public class TimerStateRepository {

    private final MutableLiveData<TimerUiState> state;

    public TimerStateRepository(TimerSettingsRepository timerSettings) {
        long defaultMs = TimerSettingsRepository.getFactoryDefaultMs();
        state = new MutableLiveData<>(TimerUiState.idle(defaultMs));
    }

    public LiveData<TimerUiState> getState() {
        return state;
    }

    public TimerUiState getCurrentState() {
        return state.getValue();
    }

    public void publish(long timeLeftMillis, boolean running, boolean paused, int sessionType,
                        boolean awaitingPostBreakChoice, boolean longBreak) {
        publish(timeLeftMillis, running, paused, sessionType, awaitingPostBreakChoice, longBreak,
                0L, 0, 0L, true, false);
    }

    public void publish(long timeLeftMillis, boolean running, boolean paused, int sessionType,
                        boolean awaitingPostBreakChoice, boolean longBreak,
                        long sessionId, int generation,
                        long pauseTimeoutRemainingMs, boolean exactAlarmReliable,
                        boolean canPause) {
        publish(timeLeftMillis, running, paused, sessionType, awaitingPostBreakChoice, longBreak,
                sessionId, generation, pauseTimeoutRemainingMs, exactAlarmReliable, canPause, true, 0);
    }

    public void publish(long timeLeftMillis, boolean running, boolean paused, int sessionType,
                        boolean awaitingPostBreakChoice, boolean longBreak,
                        long sessionId, int generation,
                        long pauseTimeoutRemainingMs, boolean exactAlarmReliable,
                        boolean canPause, boolean pauseReasonSettled, int pauseCount) {
        state.postValue(new TimerUiState(
                timeLeftMillis, running, paused, sessionType, awaitingPostBreakChoice, longBreak,
                sessionId, generation, pauseTimeoutRemainingMs, exactAlarmReliable, canPause,
                pauseReasonSettled, pauseCount));
    }

    public void syncFromService(TimerService service) {
        if (service == null) {
            return;
        }
        service.publishStateForSync();
    }
}
