package com.skyinit.pomodorotimer.ui.statistics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;

/**
 * 统计页激励/对比文案，与 UI 解耦，便于单测与复用。
 */
public final class StatisticsCopywriter {

    private static final int[] MILESTONES = {10, 25, 50, 100, 200, 500, 1000, 2000, 5000};

    private StatisticsCopywriter() {
    }

    @NonNull
    public static String weekCompare(@NonNull Context context,
                                     @NonNull StatisticsRepository.WeeklyStats week,
                                     @NonNull StatisticsRepository.WeeklyStats lastWeek) {
        long thisWeek = Math.max(0L, week.totalDuration);
        long prevWeek = Math.max(0L, lastWeek.totalDuration);
        if (thisWeek == 0L && prevWeek == 0L) {
            return context.getString(R.string.statistics_compare_both_empty);
        }
        if (prevWeek == 0L) {
            return context.getString(R.string.statistics_compare_first_week);
        }
        if (thisWeek == 0L) {
            return context.getString(R.string.statistics_compare_behind);
        }
        float ratio = (thisWeek - prevWeek) * 100f / prevWeek;
        if (Math.abs(ratio) < 1f) {
            return context.getString(R.string.statistics_compare_flat);
        }
        if (ratio > 0f) {
            return context.getString(R.string.statistics_compare_up, Math.round(ratio));
        }
        return context.getString(R.string.statistics_compare_down, Math.round(-ratio));
    }

    @NonNull
    public static String streakLabel(@NonNull Context context, int streak) {
        if (streak <= 0) {
            return context.getString(R.string.statistics_streak_zero);
        }
        return context.getString(R.string.statistics_streak_days, streak);
    }

    @NonNull
    public static String motivation(@NonNull Context context, @NonNull StatisticsDashboard dashboard) {
        if (dashboard.today.count > 0) {
            return context.getString(R.string.statistics_motivation_today_done);
        }
        if (dashboard.currentStreak > 0) {
            return context.getString(R.string.statistics_motivation_keep_streak, dashboard.currentStreak);
        }
        if (dashboard.totalCompletedCount > 0) {
            return context.getString(R.string.statistics_motivation_start_today);
        }
        return context.getString(R.string.statistics_motivation_first);
    }

    @NonNull
    public static String milestone(@NonNull Context context, int totalCompleted) {
        int safe = Math.max(0, totalCompleted);
        for (int milestone : MILESTONES) {
            if (safe < milestone) {
                int remain = milestone - safe;
                return context.getString(R.string.statistics_milestone_progress, safe, remain, milestone);
            }
        }
        return context.getString(R.string.statistics_milestone_max, safe);
    }

    @Nullable
    public static String peakHourText(@NonNull Context context,
                                      @Nullable StatisticsDashboard.PeakHourInsight insight) {
        if (insight == null) {
            return null;
        }
        int start = insight.hour;
        int end = (insight.hour + 1) % 24;
        return context.getString(R.string.statistics_insight_peak_hour, start, end);
    }

    @Nullable
    public static String topCategoryText(@NonNull Context context,
                                         @Nullable StatisticsDashboard.TopCategoryInsight insight) {
        if (insight == null) {
            return null;
        }
        return context.getString(R.string.statistics_insight_top_category,
                insight.category, Math.round(insight.percent));
    }
}
