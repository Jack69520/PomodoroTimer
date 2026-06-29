package com.skyinit.pomodorotimer.ui.calendar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import android.app.Application;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.repository.SessionRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 日历页 ViewModel：管理选中日期、当日会话与高亮日期。
 */
public class CalendarViewModel extends ViewModel {

    public static class DayStatistics {
        public final int completedSessions;
        public final long totalDurationMs;

        public DayStatistics(int completedSessions, long totalDurationMs) {
            this.completedSessions = completedSessions;
            this.totalDurationMs = totalDurationMs;
        }
    }

    private final Application application;
    private final SessionRepository sessionRepository;
    private final UserSessionRepository userSessionRepository;
    private final MutableLiveData<Calendar> selectedDate = new MutableLiveData<>(Calendar.getInstance());
    private final MediatorLiveData<List<PomodoroSession>> sessions = new MediatorLiveData<>();
    private final MutableLiveData<DayStatistics> dayStatistics = new MutableLiveData<>(new DayStatistics(0, 0));
    private final MutableLiveData<Set<String>> highlightedDates = new MutableLiveData<>();
    private final MutableLiveData<String> selectedDateLabel = new MutableLiveData<>();

    private LiveData<List<PomodoroSession>> activeSessionsSource;
    private final Observer<Integer> sessionVersionObserver = unused -> refresh();

    public CalendarViewModel(Application application,
                             SessionRepository sessionRepository,
                             UserSessionRepository userSessionRepository) {
        this.application = application;
        this.sessionRepository = sessionRepository;
        this.userSessionRepository = userSessionRepository;
        userSessionRepository.getSessionVersion().observeForever(sessionVersionObserver);
        Calendar today = Calendar.getInstance();
        updateSelectedDateLabel(today);
        bindSessionsForDate(today);
    }

    public LiveData<List<PomodoroSession>> getSessions() {
        return sessions;
    }

    public LiveData<DayStatistics> getDayStatistics() {
        return dayStatistics;
    }

    public LiveData<Set<String>> getHighlightedDates() {
        return highlightedDates;
    }

    public LiveData<String> getSelectedDateLabel() {
        return selectedDateLabel;
    }

    public LiveData<Calendar> getSelectedDate() {
        return selectedDate;
    }

    public void selectDate(Calendar date) {
        Calendar copy = Calendar.getInstance();
        copy.setTime(date.getTime());
        selectedDate.setValue(copy);
        updateSelectedDateLabel(copy);
        bindSessionsForDate(copy);
    }

    public void refresh() {
        Calendar date = selectedDate.getValue();
        if (date == null) {
            date = Calendar.getInstance();
        }
        bindSessionsForDate(date);
        loadHighlightedDates(date.get(Calendar.YEAR), date.get(Calendar.MONTH));
    }

    public void loadHighlightedDates(int year, int month) {
        sessionRepository.loadHighlightedDates(year, month, dates -> highlightedDates.setValue(dates));
    }

    private void bindSessionsForDate(Calendar date) {
        if (activeSessionsSource != null) {
            sessions.removeSource(activeSessionsSource);
            activeSessionsSource = null;
        }

        if (!sessionRepository.hasActiveProfile()) {
            sessions.setValue(new ArrayList<>());
            dayStatistics.setValue(new DayStatistics(0, 0));
            return;
        }

        long[] range = SessionRepository.dayRangeMillis(date);
        activeSessionsSource = sessionRepository.observeSessionsForDay(range[0], range[1]);
        if (activeSessionsSource != null) {
            sessions.addSource(activeSessionsSource, list -> {
                List<PomodoroSession> safeList = list == null ? new ArrayList<>() : list;
                sessions.setValue(safeList);
                dayStatistics.setValue(computeStatistics(safeList));
            });
        }
    }

    private void updateSelectedDateLabel(Calendar date) {
        String pattern = application.getString(R.string.format_date_full);
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        selectedDateLabel.setValue(sdf.format(date.getTime()));
    }

    private static DayStatistics computeStatistics(List<PomodoroSession> sessionList) {
        int completed = 0;
        long totalDuration = 0;
        for (PomodoroSession session : sessionList) {
            if (session.completed) {
                completed++;
                totalDuration += session.duration;
            }
        }
        return new DayStatistics(completed, totalDuration);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userSessionRepository.getSessionVersion().removeObserver(sessionVersionObserver);
        if (activeSessionsSource != null) {
            sessions.removeSource(activeSessionsSource);
        }
    }
}
