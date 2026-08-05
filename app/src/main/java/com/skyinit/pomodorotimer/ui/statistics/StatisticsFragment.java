package com.skyinit.pomodorotimer.ui.statistics;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.ui.statistics.chart.MonthlyFocusAreaChartView;
import com.skyinit.pomodorotimer.ui.statistics.chart.StatisticsChartTheme;
import com.skyinit.pomodorotimer.util.CategoryDefaults;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 统计页 UI：只负责绑定 {@link StatisticsViewModel} 快照与图表渲染，不含数据查询。
 */
public class StatisticsFragment extends Fragment {

    private TextView heroTodayDuration;
    private TextView heroTodaySessions;
    private TextView heroWeekDuration;
    private TextView heroWeekSessions;
    private TextView heroMonthDuration;
    private TextView heroMonthSessions;
    private TextView heroStreak;
    private TextView heroCompare;
    private TextView heroMilestone;
    private TextView heroMotivation;
    private TextView insightPeakHour;
    private TextView insightTopCategory;
    private TextView insightEmpty;
    private MonthlyFocusAreaChartView monthlyAreaChart;
    private TextView monthlyChartEmptyText;
    private BarChart weekBarChart;
    private TextView weekChartEmptyText;
    private PieChart categoryPieChart;
    private TextView categoryChartEmptyText;
    private BarChart hourlyDistributionChart;
    private TextView hourlyDistributionEmptyText;
    private HorizontalBarChart pauseReasonChart;
    private TextView pauseReasonChartEmptyText;
    private LinearLayout pauseSectionHeader;
    private LinearLayout pauseSectionBody;
    private TextView pauseSectionToggle;
    private ProgressBar loadingView;

    private StatisticsViewModel viewModel;
    private boolean pauseSectionExpanded;
    private boolean categoryDrillDownDialogShowing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        initViewModel();
        setupStaticCharts();
        setupPauseSectionToggle();
        observeViewModel();
    }

    private void bindViews(@NonNull View view) {
        heroTodayDuration = view.findViewById(R.id.hero_today_duration);
        heroTodaySessions = view.findViewById(R.id.hero_today_sessions);
        heroWeekDuration = view.findViewById(R.id.hero_week_duration);
        heroWeekSessions = view.findViewById(R.id.hero_week_sessions);
        heroMonthDuration = view.findViewById(R.id.hero_month_duration);
        heroMonthSessions = view.findViewById(R.id.hero_month_sessions);
        heroStreak = view.findViewById(R.id.hero_streak);
        heroCompare = view.findViewById(R.id.hero_compare);
        heroMilestone = view.findViewById(R.id.hero_milestone);
        heroMotivation = view.findViewById(R.id.hero_motivation);
        insightPeakHour = view.findViewById(R.id.insight_peak_hour);
        insightTopCategory = view.findViewById(R.id.insight_top_category);
        insightEmpty = view.findViewById(R.id.insight_empty);
        monthlyAreaChart = view.findViewById(R.id.monthly_area_chart);
        monthlyChartEmptyText = view.findViewById(R.id.monthly_chart_empty_text);
        weekBarChart = view.findViewById(R.id.week_bar_chart);
        weekChartEmptyText = view.findViewById(R.id.week_chart_empty_text);
        categoryPieChart = view.findViewById(R.id.category_pie_chart);
        categoryChartEmptyText = view.findViewById(R.id.category_chart_empty_text);
        hourlyDistributionChart = view.findViewById(R.id.hourly_distribution_chart);
        hourlyDistributionEmptyText = view.findViewById(R.id.hourly_distribution_empty_text);
        pauseReasonChart = view.findViewById(R.id.pause_reason_chart);
        pauseReasonChartEmptyText = view.findViewById(R.id.pause_reason_chart_empty_text);
        pauseSectionHeader = view.findViewById(R.id.pause_section_header);
        pauseSectionBody = view.findViewById(R.id.pause_section_body);
        pauseSectionToggle = view.findViewById(R.id.pause_section_toggle);
        loadingView = view.findViewById(R.id.statistics_loading);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this,
                ((App) requireActivity().getApplication()).getContainer().getViewModelFactory())
                .get(StatisticsViewModel.class);
    }

    private void setupStaticCharts() {
        StatisticsChartTheme.applyCartesianChrome(weekBarChart, requireContext());
        weekBarChart.setTouchEnabled(true);
        weekBarChart.setDragEnabled(false);
        weekBarChart.getXAxis().setGranularity(1f);
        weekBarChart.getXAxis().setLabelCount(7, false);

        StatisticsChartTheme.applyPieChrome(categoryPieChart, requireContext());
        categoryPieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                if (categoryDrillDownDialogShowing || !(e instanceof PieEntry)) {
                    return;
                }
                String label = ((PieEntry) e).getLabel();
                if (label != null && !label.isEmpty()) {
                    viewModel.loadCategoryDrillDown(label);
                }
            }

            @Override
            public void onNothingSelected() {
            }
        });

        StatisticsChartTheme.applyCartesianChrome(hourlyDistributionChart, requireContext());
        hourlyDistributionChart.setTouchEnabled(true);
        hourlyDistributionChart.setDragEnabled(false);
        XAxis hourlyX = hourlyDistributionChart.getXAxis();
        hourlyX.setGranularity(1f);
        hourlyX.setAxisMinimum(-0.5f);
        hourlyX.setAxisMaximum(23.5f);
        hourlyX.setLabelCount(9, true);
        hourlyX.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int hour = Math.round(value);
                if (hour == 0 || hour == 3 || hour == 6 || hour == 9 || hour == 12
                        || hour == 15 || hour == 18 || hour == 21) {
                    return getString(R.string.statistics_label_hour, hour);
                }
                return "";
            }
        });

        StatisticsChartTheme.applyCartesianChrome(pauseReasonChart, requireContext());
        pauseReasonChart.setFitBars(true);
        pauseReasonChart.setTouchEnabled(true);
        pauseReasonChart.setDragEnabled(false);
    }

    private void setupPauseSectionToggle() {
        pauseSectionHeader.setOnClickListener(v -> {
            pauseSectionExpanded = !pauseSectionExpanded;
            pauseSectionBody.setVisibility(pauseSectionExpanded ? View.VISIBLE : View.GONE);
            pauseSectionToggle.setText(pauseSectionExpanded
                    ? R.string.statistics_pause_section_collapse
                    : R.string.statistics_pause_section_expand);
            if (pauseSectionExpanded && pauseReasonChart.getData() != null) {
                pauseReasonChart.animateY(400);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loadingView != null) {
                loadingView.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getDashboard().observe(getViewLifecycleOwner(), this::bindDashboard);

        viewModel.getCategoryDrillDown().observe(getViewLifecycleOwner(), event -> {
            if (event == null || !isAdded() || categoryDrillDownDialogShowing) {
                return;
            }
            showCategoryDrillDownDialog(event.category, event.sessions);
            viewModel.clearCategoryDrillDown();
        });
    }

    private void bindDashboard(@Nullable StatisticsDashboard dashboard) {
        if (dashboard == null || !isAdded()) {
            return;
        }
        bindHero(dashboard);
        bindInsights(dashboard);
        bindMonthlyArea(dashboard.monthlyDays);
        bindWeekBars(dashboard.weeklyDays);
        bindCategoryPie(dashboard.categoryStats);
        bindHourlyBars(dashboard.hourlyStats);
        bindPauseBars(dashboard.pauseReasonStats);
    }

    private void bindHero(@NonNull StatisticsDashboard dashboard) {
        heroTodayDuration.setText(formatDuration(dashboard.today.totalDuration));
        heroTodaySessions.setText(getString(R.string.statistics_hero_sessions_format, dashboard.today.count));
        heroWeekDuration.setText(formatDuration(dashboard.week.totalDuration));
        heroWeekSessions.setText(getString(R.string.statistics_hero_sessions_format, dashboard.week.totalSessions));
        heroMonthDuration.setText(formatDuration(dashboard.month.totalDuration));
        heroMonthSessions.setText(getString(
                R.string.statistics_active_days_month, dashboard.activeDaysThisMonth)
                + " · "
                + getString(R.string.statistics_hero_sessions_format, dashboard.month.totalSessions));
        heroStreak.setText(StatisticsCopywriter.streakLabel(requireContext(), dashboard.currentStreak));
        heroCompare.setText(StatisticsCopywriter.weekCompare(
                requireContext(), dashboard.week, dashboard.lastWeek));
        heroMilestone.setText(StatisticsCopywriter.milestone(
                requireContext(), dashboard.totalCompletedCount));
        heroMotivation.setText(StatisticsCopywriter.motivation(requireContext(), dashboard));
    }

    private void bindInsights(@NonNull StatisticsDashboard dashboard) {
        String peak = StatisticsCopywriter.peakHourText(requireContext(), dashboard.peakHourInsight);
        String top = StatisticsCopywriter.topCategoryText(requireContext(), dashboard.topCategoryInsight);
        boolean hasInsight = peak != null || top != null;
        insightEmpty.setVisibility(hasInsight ? View.GONE : View.VISIBLE);
        if (peak != null) {
            insightPeakHour.setVisibility(View.VISIBLE);
            insightPeakHour.setText(peak);
        } else {
            insightPeakHour.setVisibility(View.GONE);
        }
        if (top != null) {
            insightTopCategory.setVisibility(View.VISIBLE);
            insightTopCategory.setText(top);
        } else {
            insightTopCategory.setVisibility(View.GONE);
        }
    }

    private void bindMonthlyArea(@NonNull List<DailyStats> monthlyDays) {
        float[] minutes = new float[monthlyDays.size()];
        boolean hasData = false;
        for (int i = 0; i < monthlyDays.size(); i++) {
            float value = monthlyDays.get(i).totalDuration / (1000f * 60f);
            minutes[i] = value;
            if (value > 0f) {
                hasData = true;
            }
        }
        if (!hasData) {
            monthlyAreaChart.setVisibility(View.GONE);
            monthlyChartEmptyText.setVisibility(View.VISIBLE);
            monthlyAreaChart.setMonthData(new float[0], false);
            return;
        }
        monthlyAreaChart.setVisibility(View.VISIBLE);
        monthlyChartEmptyText.setVisibility(View.GONE);
        monthlyAreaChart.setMonthData(minutes, true);
    }

    private void bindWeekBars(@NonNull List<DailyStats> weeklyDays) {
        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        SimpleDateFormat labelFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        float max = 0f;
        boolean hasData = false;
        for (int i = 0; i < weeklyDays.size(); i++) {
            DailyStats day = weeklyDays.get(i);
            float minutes = day.totalDuration / (1000f * 60f);
            entries.add(new BarEntry(i, minutes));
            String label = day.date;
            try {
                Date parsed = parseFormat.parse(day.date);
                if (parsed != null) {
                    label = labelFormat.format(parsed);
                }
            } catch (Exception ignored) {
            }
            labels.add(label);
            max = Math.max(max, minutes);
            if (minutes > 0f) {
                hasData = true;
            }
        }
        if (!hasData) {
            weekBarChart.setVisibility(View.GONE);
            weekChartEmptyText.setVisibility(View.VISIBLE);
            return;
        }
        weekBarChart.setVisibility(View.VISIBLE);
        weekChartEmptyText.setVisibility(View.GONE);

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(StatisticsChartTheme.weekBar(requireContext()));
        dataSet.setDrawValues(false);
        dataSet.setHighLightAlpha(80);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.55f);
        weekBarChart.setData(data);
        weekBarChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        YAxis left = weekBarChart.getAxisLeft();
        left.setAxisMaximum(max > 0f ? max * 1.2f : 60f);
        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return getString(R.string.format_duration_minutes_float, value);
            }
        });
        weekBarChart.invalidate();
    }

    private void bindCategoryPie(@NonNull List<CategoryStats> categoryStats) {
        if (categoryStats.isEmpty()) {
            categoryPieChart.setVisibility(View.GONE);
            categoryChartEmptyText.setVisibility(View.VISIBLE);
            return;
        }
        categoryPieChart.setVisibility(View.VISIBLE);
        categoryChartEmptyText.setVisibility(View.GONE);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (CategoryStats stats : categoryStats) {
            if (stats.totalDuration <= 0L) {
                continue;
            }
            String label = stats.category != null && !stats.category.isEmpty()
                    ? stats.category : CategoryDefaults.getDefault();
            entries.add(new PieEntry(stats.totalDuration / (1000f * 60f), label));
            colors.add(StatisticsChartTheme.categorySlice(requireContext(), label));
        }
        if (entries.isEmpty()) {
            categoryPieChart.setVisibility(View.GONE);
            categoryChartEmptyText.setVisibility(View.VISIBLE);
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(6f);

        categoryPieChart.setData(new PieData(dataSet));
        StatisticsChartTheme.applyPieChrome(categoryPieChart, requireContext());
        categoryPieChart.invalidate();
    }

    private void bindHourlyBars(@NonNull List<HourlyStats> hourlyStats) {
        float[] minutesByHour = new float[24];
        float max = 0f;
        boolean hasData = false;
        for (HourlyStats stats : hourlyStats) {
            if (stats == null || stats.hour < 0 || stats.hour > 23) {
                continue;
            }
            float minutes = stats.totalDuration / (1000f * 60f);
            minutesByHour[stats.hour] = minutes;
            max = Math.max(max, minutes);
            if (minutes > 0f) {
                hasData = true;
            }
        }
        if (!hasData) {
            hourlyDistributionChart.setVisibility(View.GONE);
            hourlyDistributionEmptyText.setVisibility(View.VISIBLE);
            return;
        }
        hourlyDistributionChart.setVisibility(View.VISIBLE);
        hourlyDistributionEmptyText.setVisibility(View.GONE);

        List<BarEntry> entries = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            entries.add(new BarEntry(hour, minutesByHour[hour]));
        }
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(StatisticsChartTheme.hourlyBar(requireContext()));
        dataSet.setDrawValues(false);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);
        hourlyDistributionChart.setData(data);
        hourlyDistributionChart.getAxisLeft().setAxisMaximum(max > 0f ? max * 1.2f : 60f);
        hourlyDistributionChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return getString(R.string.format_duration_minutes_float, value);
            }
        });
        hourlyDistributionChart.invalidate();
    }

    private void bindPauseBars(@NonNull List<PauseReasonStats> statsList) {
        if (statsList.isEmpty()) {
            pauseReasonChart.setVisibility(View.GONE);
            pauseReasonChartEmptyText.setVisibility(View.VISIBLE);
            return;
        }
        pauseReasonChart.setVisibility(View.VISIBLE);
        pauseReasonChartEmptyText.setVisibility(View.GONE);

        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        float max = 0f;
        // HorizontalBarChart 自下而上绘制，反转顺序让最高的在顶部。
        for (int i = statsList.size() - 1; i >= 0; i--) {
            PauseReasonStats stats = statsList.get(i);
            float count = stats.count;
            entries.add(new BarEntry(statsList.size() - 1 - i, count));
            labels.add(stats.pauseReason != null
                    ? stats.pauseReason
                    : getString(R.string.session_detail_none));
            max = Math.max(max, count);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(StatisticsChartTheme.pauseBar(requireContext()));
        dataSet.setDrawValues(false);
        pauseReasonChart.setData(new BarData(dataSet));
        pauseReasonChart.getXAxis().setGranularity(1f);
        pauseReasonChart.getXAxis().setLabelCount(labels.size());
        pauseReasonChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        pauseReasonChart.getAxisLeft().setAxisMaximum(max > 0f ? max * 1.2f : 5f);
        pauseReasonChart.getAxisLeft().setGranularity(1f);
        pauseReasonChart.invalidate();
    }

    private void showCategoryDrillDownDialog(@NonNull String category,
                                             @NonNull List<PomodoroSession> sessions) {
        if (!isAdded() || categoryDrillDownDialogShowing) {
            return;
        }
        categoryDrillDownDialogShowing = true;
        long totalDuration = 0L;
        int count = 0;
        StringBuilder message = new StringBuilder();
        SimpleDateFormat timeFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        for (PomodoroSession session : sessions) {
            if (session == null || !session.completed) {
                continue;
            }
            count++;
            totalDuration += session.duration;
            message.append(timeFormat.format(new Date(session.startTime)))
                    .append(" · ")
                    .append(formatDuration(session.duration))
                    .append('\n');
        }
        if (count == 0) {
            message.append(getString(R.string.statistics_empty_no_records));
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.statistics_category_drilldown_title, category))
                .setMessage(getString(R.string.statistics_category_drilldown_summary,
                        count, formatDuration(totalDuration)) + "\n\n" + message)
                .setPositiveButton(R.string.confirm, null)
                .create();
        dialog.setOnDismissListener(d -> {
            categoryDrillDownDialogShowing = false;
            clearCategoryPieSelection();
        });
        dialog.show();
    }

    private void clearCategoryPieSelection() {
        if (categoryPieChart != null) {
            categoryPieChart.highlightValues(null);
            categoryPieChart.invalidate();
        }
    }

    private String formatDuration(long durationMs) {
        long minutes = Math.max(0L, durationMs) / (1000 * 60);
        long hours = minutes / 60;
        minutes = minutes % 60;
        if (hours > 0) {
            return getString(R.string.format_duration_hours_minutes, hours, minutes);
        }
        return getString(R.string.format_duration_minutes, minutes);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyThemeToUi();
        if (viewModel != null && viewModel.getDashboard().getValue() != null) {
            bindDashboard(viewModel.getDashboard().getValue());
        }
    }

    private void applyThemeToUi() {
        if (getView() == null || !isAdded()) {
            return;
        }
        int primaryText = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int secondaryText = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        heroTodayDuration.setTextColor(primaryText);
        heroTodaySessions.setTextColor(secondaryText);
        heroWeekDuration.setTextColor(primaryText);
        heroMonthDuration.setTextColor(primaryText);
        heroCompare.setTextColor(secondaryText);
        heroMotivation.setTextColor(primaryText);
        monthlyChartEmptyText.setTextColor(secondaryText);
        weekChartEmptyText.setTextColor(secondaryText);
        categoryChartEmptyText.setTextColor(secondaryText);
        hourlyDistributionEmptyText.setTextColor(secondaryText);
        pauseReasonChartEmptyText.setTextColor(secondaryText);
        if (monthlyAreaChart != null) {
            monthlyAreaChart.applyThemeColors();
        }
        setupStaticCharts();
    }
}
