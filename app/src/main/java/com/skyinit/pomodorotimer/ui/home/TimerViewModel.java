package com.skyinit.pomodorotimer.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerStateRepository;
import com.skyinit.pomodorotimer.service.TimerService;

import java.util.Locale;

public class TimerViewModel extends ViewModel {

    private final TimerSettingsRepository timerSettings;
    private final UserSessionRepository sessionRepository;
    private final TimerStateRepository timerStateRepository;

    public TimerViewModel(TimerSettingsRepository timerSettings,
                          UserSessionRepository sessionRepository,
                          TimerStateRepository timerStateRepository) {
        this.timerSettings = timerSettings;
        this.sessionRepository = sessionRepository;
        this.timerStateRepository = timerStateRepository;
    }

    public LiveData<TimerUiState> getTimerState() {
        return timerStateRepository.getState();
    }

    public boolean isLoggedIn() {
        return sessionRepository.isRegistered();
    }

    public void syncFromService(TimerService service) {
        timerStateRepository.syncFromService(service);
    }

    public String formatTime(long millis) {
        long totalSeconds = millis / 1000L;
        if (totalSeconds >= 3600L) {
            long hours = totalSeconds / 3600L;
            long minutes = (totalSeconds % 3600L) / 60L;
            long seconds = totalSeconds % 60L;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public int resolveSessionHintResId(TimerUiState state) {
        if (state == null) {
            return 0;
        }
        if (state.awaitingPostBreakChoice) {
            return com.skyinit.pomodorotimer.R.string.timer_post_break_hint;
        }
        if (state.isBreakSession() && state.running) {
            return state.longBreak
                    ? com.skyinit.pomodorotimer.R.string.timer_long_break_hint
                    : com.skyinit.pomodorotimer.R.string.timer_break_running_hint;
        }
        return 0;
    }

    public boolean shouldShowPauseControls(TimerUiState state) {
        return state != null && state.isStudySession() && (state.running || state.paused);
    }

    public boolean shouldShowBreakEndButton(TimerUiState state) {
        return state != null && state.isBreakSession() && state.running;
    }

    public boolean shouldShowPostBreakActions(TimerUiState state) {
        return state != null && state.awaitingPostBreakChoice;
    }

    public boolean shouldShowIdleStudyControls(TimerUiState state) {
        return state != null && !state.running && !state.paused
                && !state.awaitingPostBreakChoice && state.isStudySession();
    }
}
