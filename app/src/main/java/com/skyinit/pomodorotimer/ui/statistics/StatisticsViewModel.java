package com.skyinit.pomodorotimer.ui.statistics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统计页 ViewModel：单一 dashboard 快照 + generation 丢弃过期异步结果，避免竞态。
 */
public class StatisticsViewModel extends ViewModel {

    private final StatisticsRepository statisticsRepository;
    private final UserSessionRepository sessionRepository;

    private final MutableLiveData<StatisticsDashboard> dashboard = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<CategoryDrillDownEvent> categoryDrillDown = new MutableLiveData<>();

    private final AtomicInteger refreshGeneration = new AtomicInteger(0);
    private final AtomicInteger drillDownGeneration = new AtomicInteger(0);

    private final Observer<Integer> sessionVersionObserver = unused -> refresh();

    public StatisticsViewModel(StatisticsRepository statisticsRepository,
                               UserSessionRepository sessionRepository) {
        this.statisticsRepository = statisticsRepository;
        this.sessionRepository = sessionRepository;
        sessionRepository.getSessionVersion().observeForever(sessionVersionObserver);
        refresh();
    }

    @NonNull
    public LiveData<StatisticsDashboard> getDashboard() {
        return dashboard;
    }

    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    @NonNull
    public LiveData<CategoryDrillDownEvent> getCategoryDrillDown() {
        return categoryDrillDown;
    }

    public void refresh() {
        if (!sessionRepository.hasActiveProfile()) {
            return;
        }
        final int generation = refreshGeneration.incrementAndGet();
        // 仅首次无数据时展示 loading，避免 onResume 刷新造成整页闪烁。
        if (dashboard.getValue() == null) {
            loading.setValue(true);
        }
        statisticsRepository.loadDashboard(result -> {
            if (generation != refreshGeneration.get()) {
                return;
            }
            dashboard.setValue(result);
            loading.setValue(false);
        });
    }

    public void loadCategoryDrillDown(@Nullable String category) {
        if (category == null || category.isEmpty() || !sessionRepository.hasActiveProfile()) {
            return;
        }
        final int generation = drillDownGeneration.incrementAndGet();
        final String requested = category;
        statisticsRepository.getSessionsByCategoryThisMonth(requested, sessions -> {
            if (generation != drillDownGeneration.get()) {
                return;
            }
            List<PomodoroSession> safe = sessions != null ? sessions : Collections.emptyList();
            categoryDrillDown.setValue(new CategoryDrillDownEvent(requested, safe));
        });
    }

    /** 弹窗展示后清空，避免配置变更时重复弹出。 */
    public void clearCategoryDrillDown() {
        categoryDrillDown.setValue(null);
    }

    @Override
    protected void onCleared() {
        refreshGeneration.incrementAndGet();
        drillDownGeneration.incrementAndGet();
        sessionRepository.getSessionVersion().removeObserver(sessionVersionObserver);
        super.onCleared();
    }

    public static final class CategoryDrillDownEvent {
        @NonNull
        public final String category;
        @NonNull
        public final List<PomodoroSession> sessions;

        public CategoryDrillDownEvent(@NonNull String category,
                                      @NonNull List<PomodoroSession> sessions) {
            this.category = category;
            this.sessions = sessions;
        }
    }
}
