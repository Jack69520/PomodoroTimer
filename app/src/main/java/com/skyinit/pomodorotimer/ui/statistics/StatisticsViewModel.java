package com.skyinit.pomodorotimer.ui.statistics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计页 ViewModel：封装 StatisticsRepository 的回调为 LiveData，并在档案切换时自动刷新。
 */
public class StatisticsViewModel extends ViewModel {

    private final StatisticsRepository statisticsRepository;
    private final UserSessionRepository sessionRepository;

    private final MutableLiveData<DailyStats> todayStats = new MutableLiveData<>();
    private final MutableLiveData<StatisticsRepository.WeeklyStats> weekStats = new MutableLiveData<>();
    private final MutableLiveData<StatisticsRepository.MonthlyStats> monthStats = new MutableLiveData<>();
    private final MutableLiveData<List<DailyStats>> weeklyChartData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<DailyStats>> monthlyChartData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<HourlyStats>> hourlyChartData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CategoryStats>> categoryStats = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PauseReasonStats>> pauseReasonStats = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PomodoroSession>> categoryDrillDownSessions = new MutableLiveData<>();

    private final Observer<Integer> sessionVersionObserver = unused -> refresh();

    public StatisticsViewModel(StatisticsRepository statisticsRepository,
                               UserSessionRepository sessionRepository) {
        this.statisticsRepository = statisticsRepository;
        this.sessionRepository = sessionRepository;
        sessionRepository.getSessionVersion().observeForever(sessionVersionObserver);
        refresh();
    }

    public LiveData<DailyStats> getTodayStats() {
        return todayStats;
    }

    public LiveData<StatisticsRepository.WeeklyStats> getWeekStats() {
        return weekStats;
    }

    public LiveData<StatisticsRepository.MonthlyStats> getMonthStats() {
        return monthStats;
    }

    public LiveData<List<DailyStats>> getWeeklyChartData() {
        return weeklyChartData;
    }

    public LiveData<List<DailyStats>> getMonthlyChartData() {
        return monthlyChartData;
    }

    public LiveData<List<HourlyStats>> getHourlyChartData() {
        return hourlyChartData;
    }

    public LiveData<List<CategoryStats>> getCategoryStats() {
        return categoryStats;
    }

    public LiveData<List<PauseReasonStats>> getPauseReasonStats() {
        return pauseReasonStats;
    }

    public LiveData<List<PomodoroSession>> getCategoryDrillDownSessions() {
        return categoryDrillDownSessions;
    }

    public void refresh() {
        if (!sessionRepository.hasActiveProfile()) {
            return;
        }

        statisticsRepository.getTodayStats(todayStats::setValue);
        statisticsRepository.getThisWeekStats(weekStats::setValue);
        statisticsRepository.getThisMonthStats(monthStats::setValue);
        statisticsRepository.getWeeklyChartData(weeklyChartData::setValue);
        statisticsRepository.getMonthlyChartData(monthlyChartData::setValue);
        statisticsRepository.getMonthlyHourlyDistribution(hourlyChartData::setValue);
        statisticsRepository.getMonthlyCategoryStats(categoryStats::setValue);
        statisticsRepository.getMonthlyPauseReasonStats(pauseReasonStats::setValue);
    }

    public void loadCategoryDrillDown(String category) {
        statisticsRepository.getSessionsByCategoryThisMonth(category, categoryDrillDownSessions::setValue);
    }

    @Override
    protected void onCleared() {
        sessionRepository.getSessionVersion().removeObserver(sessionVersionObserver);
        super.onCleared();
    }
}
