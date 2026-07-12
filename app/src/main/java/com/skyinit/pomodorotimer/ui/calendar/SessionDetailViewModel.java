package com.skyinit.pomodorotimer.ui.calendar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.data.repository.SessionRepository;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;

/**
 * 专注详情页 ViewModel：加载会话与保存备注。
 */
public class SessionDetailViewModel extends ViewModel {

    public static class SaveNotesResult {
        public final boolean success;
        public final String message;

        public SaveNotesResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private final SessionRepository sessionRepository;
    private final StatisticsRepository statisticsRepository;
    private final UserSessionRepository userSessionRepository;

    private final MutableLiveData<PomodoroSession> session = new MutableLiveData<>();
    private final MutableLiveData<SaveNotesResult> saveNotesResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> accessDenied = new MutableLiveData<>(false);

    private LiveData<PomodoroSession> sessionSource;
    private int sessionId = -1;

    public SessionDetailViewModel(SessionRepository sessionRepository,
                                  StatisticsRepository statisticsRepository,
                                  UserSessionRepository userSessionRepository) {
        this.sessionRepository = sessionRepository;
        this.statisticsRepository = statisticsRepository;
        this.userSessionRepository = userSessionRepository;
    }

    public LiveData<PomodoroSession> getSession() {
        return session;
    }

    public LiveData<SaveNotesResult> getSaveNotesResult() {
        return saveNotesResult;
    }

    public LiveData<Boolean> getAccessDenied() {
        return accessDenied;
    }

    public void loadSession(int id) {
        sessionId = id;
        if (id <= 0) {
            accessDenied.setValue(true);
            return;
        }
        sessionSource = sessionRepository.observeSession(id);
    }

    public LiveData<PomodoroSession> getSessionSource() {
        return sessionSource;
    }

    public boolean validateSession(PomodoroSession loaded) {
        if (loaded == null) {
            accessDenied.setValue(true);
            return false;
        }
        String userId = userSessionRepository.requireActiveUserId();
        if (!userId.equals(loaded.userId)) {
            accessDenied.setValue(true);
            return false;
        }
        session.setValue(loaded);
        accessDenied.setValue(false);
        return true;
    }

    public void saveNotes(String notes) {
        PomodoroSession current = session.getValue();
        if (current == null) {
            return;
        }
        statisticsRepository.updateSessionNotes(current.id, notes, () ->
                saveNotesResult.postValue(new SaveNotesResult(true, null)));
    }
}
