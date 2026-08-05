package com.skyinit.pomodorotimer.ui.statistics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统计页一次刷新的完整快照，避免多路异步回调交错导致 UI 竞态。
 */
public final class StatisticsDashboard {

    @NonNull
    public final DailyStats today;
    @NonNull
    public final StatisticsRepository.WeeklyStats week;
    @NonNull
    public final StatisticsRepository.WeeklyStats lastWeek;
    @NonNull
    public final StatisticsRepository.MonthlyStats month;
    public final int currentStreak;
    public final int activeDaysThisMonth;
    public final int totalCompletedCount;
    /** 近 7 天，按日补齐，date=yyyy-MM-dd，共 7 项。 */
    @NonNull
    public final List<DailyStats> weeklyDays;
    /** 本月每日补齐，从 1 号到月末。 */
    @NonNull
    public final List<DailyStats> monthlyDays;
    @NonNull
    public final List<HourlyStats> hourlyStats;
    @NonNull
    public final List<CategoryStats> categoryStats;
    @NonNull
    public final List<PauseReasonStats> pauseReasonStats;
    @Nullable
    public final PeakHourInsight peakHourInsight;
    @Nullable
    public final TopCategoryInsight topCategoryInsight;

    public StatisticsDashboard(
            @NonNull DailyStats today,
            @NonNull StatisticsRepository.WeeklyStats week,
            @NonNull StatisticsRepository.WeeklyStats lastWeek,
            @NonNull StatisticsRepository.MonthlyStats month,
            int currentStreak,
            int activeDaysThisMonth,
            int totalCompletedCount,
            @NonNull List<DailyStats> weeklyDays,
            @NonNull List<DailyStats> monthlyDays,
            @NonNull List<HourlyStats> hourlyStats,
            @NonNull List<CategoryStats> categoryStats,
            @NonNull List<PauseReasonStats> pauseReasonStats,
            @Nullable PeakHourInsight peakHourInsight,
            @Nullable TopCategoryInsight topCategoryInsight) {
        this.today = today;
        this.week = week;
        this.lastWeek = lastWeek;
        this.month = month;
        this.currentStreak = Math.max(0, currentStreak);
        this.activeDaysThisMonth = Math.max(0, activeDaysThisMonth);
        this.totalCompletedCount = Math.max(0, totalCompletedCount);
        this.weeklyDays = Collections.unmodifiableList(new ArrayList<>(weeklyDays));
        this.monthlyDays = Collections.unmodifiableList(new ArrayList<>(monthlyDays));
        this.hourlyStats = Collections.unmodifiableList(new ArrayList<>(hourlyStats));
        this.categoryStats = Collections.unmodifiableList(new ArrayList<>(categoryStats));
        this.pauseReasonStats = Collections.unmodifiableList(new ArrayList<>(pauseReasonStats));
        this.peakHourInsight = peakHourInsight;
        this.topCategoryInsight = topCategoryInsight;
    }

    public static final class PeakHourInsight {
        public final int hour;
        public final long durationMs;

        public PeakHourInsight(int hour, long durationMs) {
            this.hour = hour;
            this.durationMs = durationMs;
        }
    }

    public static final class TopCategoryInsight {
        @NonNull
        public final String category;
        public final long durationMs;
        public final float percent;

        public TopCategoryInsight(@NonNull String category, long durationMs, float percent) {
            this.category = category;
            this.durationMs = durationMs;
            this.percent = percent;
        }
    }
}
