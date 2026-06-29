package com.skyinit.pomodorotimer.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerStateRepository;
import com.skyinit.pomodorotimer.service.TimerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeViewModel extends ViewModel {
    public static final int CONTROL_START = 0;
    public static final int CONTROL_PAUSE = 1;
    public static final int CONTROL_RESUME = 2;

    private final TimerSettingsRepository timerSettings;
    private final UserSessionRepository sessionRepository;
    private final TimerStateRepository timerStateRepository;

    private final MutableLiveData<Long> timerMillis;
    private final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> paused = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> sessionType = new MutableLiveData<>(0);
    private final MutableLiveData<String> timerDisplayText;
    private final MutableLiveData<Integer> controlButtonState = new MutableLiveData<>(CONTROL_START);
    private final MutableLiveData<List<TodoItem>> todos = new MutableLiveData<>(new ArrayList<>());
    private final MediatorLiveData<TimerUiState> timerStateBridge = new MediatorLiveData<>();

    public HomeViewModel(TimerSettingsRepository timerSettings,
                         UserSessionRepository sessionRepository,
                         TimerStateRepository timerStateRepository) {
        this.timerSettings = timerSettings;
        this.sessionRepository = sessionRepository;
        this.timerStateRepository = timerStateRepository;

        long defaultMs = timerSettings.getDefaultStudyTimeMs();
        timerMillis = new MutableLiveData<>(defaultMs);
        timerDisplayText = new MutableLiveData<>(formatTimerText(defaultMs, 0, defaultMs));

        timerStateBridge.addSource(timerStateRepository.getState(), state -> {
            if (state != null) {
                syncTimerState(state.timeLeftMillis, state.running, state.paused, state.sessionType);
            }
        });
    }

    public LiveData<TimerUiState> getTimerState() {
        return timerStateRepository.getState();
    }

    public LiveData<Long> getTimerMillis() {
        return timerMillis;
    }

    public LiveData<Boolean> isRunning() {
        return running;
    }

    public LiveData<Boolean> isPaused() {
        return paused;
    }

    public LiveData<Integer> getSessionType() {
        return sessionType;
    }

    public LiveData<String> getTimerDisplayText() {
        return timerDisplayText;
    }

    public LiveData<Integer> getControlButtonState() {
        return controlButtonState;
    }

    public LiveData<List<TodoItem>> getTodos() {
        return todos;
    }

    public boolean isLoggedIn() {
        return sessionRepository.isRegistered();
    }

    public void syncFromService(TimerService service) {
        timerStateRepository.syncFromService(service);
    }

    public void syncTimerState(long timeLeftMillis, boolean isRunning, boolean isPaused, int sessionTypeValue) {
        timerMillis.setValue(timeLeftMillis);
        running.setValue(isRunning);
        paused.setValue(isPaused);
        sessionType.setValue(sessionTypeValue);
        long defaultMs = timerSettings.getDefaultStudyTimeMs();
        timerDisplayText.setValue(formatTimerText(timeLeftMillis, sessionTypeValue, defaultMs));
        updateControlState(timeLeftMillis, isRunning, isPaused, sessionTypeValue);
    }

    public void setDefaultStudyTime(long millis) {
        long clamped = timerSettings.setDefaultStudyTimeMs(millis);
        timerMillis.setValue(clamped);
        timerDisplayText.setValue(formatTimerText(clamped, 0, clamped));
        updateControlState(clamped, false, false, 0);
    }

    public long resetStudyTimeToDefault() {
        long defaultMs = timerSettings.resetToDefault();
        setDefaultStudyTime(defaultMs);
        return defaultMs;
    }

    public long getDefaultStudyTimeMs() {
        return timerSettings.getDefaultStudyTimeMs();
    }

    /** 从设置页返回或外部修改学习时长后，刷新首页显示。 */
    public void refreshStudyTimeFromSettings() {
        if (Boolean.TRUE.equals(running.getValue()) || Boolean.TRUE.equals(paused.getValue())) {
            return;
        }
        if (sessionType.getValue() != null && sessionType.getValue() != 0) {
            return;
        }
        long defaultMs = timerSettings.getDefaultStudyTimeMs();
        timerMillis.setValue(defaultMs);
        timerDisplayText.setValue(formatTimerText(defaultMs, 0, defaultMs));
        updateControlState(defaultMs, false, false, 0);
    }

    public void setTodos(List<TodoItem> list) {
        todos.setValue(list == null ? new ArrayList<>() : list);
    }

    public String formatTimerText(long millis) {
        int type = sessionType.getValue() != null ? sessionType.getValue() : 0;
        return formatTimerText(millis, type, timerSettings.getDefaultStudyTimeMs());
    }

    public static String formatTimerText(long millis, int sessionTypeValue, long defaultStudyTimeMs) {
        boolean useHms = defaultStudyTimeMs >= 60L * 60L * 1000L && sessionTypeValue == 0;
        if (useHms) {
            long totalSeconds = millis / 1000L;
            long hours = totalSeconds / 3600L;
            long minutes = (totalSeconds % 3600L) / 60L;
            long seconds = totalSeconds % 60L;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void updateControlState(long timeLeft, boolean isRunning, boolean isPaused, int sessionTypeValue) {
        if (isRunning) {
            controlButtonState.setValue(CONTROL_PAUSE);
        } else if (isPaused) {
            controlButtonState.setValue(CONTROL_RESUME);
        } else {
            controlButtonState.setValue(CONTROL_START);
        }
    }
}
