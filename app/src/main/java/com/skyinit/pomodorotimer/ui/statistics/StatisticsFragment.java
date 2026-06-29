package com.skyinit.pomodorotimer.ui.statistics;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.CategoryDefaults;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatisticsFragment extends Fragment {
    private TextView todaySessionsText;
    private TextView todayDurationText;
    private TextView weekSessionsText;
    private TextView weekDurationText;
    private TextView monthSessionsText;
    private TextView monthDurationText;
    private LineChart chart;
    private TextView chartEmptyText;
    private android.widget.ImageView chartEmptyImage;
    private LineChart monthlyChart;
    private TextView monthlyChartEmptyText;
    private com.github.mikephil.charting.charts.BarChart hourlyDistributionChart;
    private TextView hourlyDistributionEmptyText;
    private PieChart categoryPieChart;
    private TextView categoryChartEmptyText;
    private BarChart pauseReasonChart;
    private TextView pauseReasonChartEmptyText;
    private List<CategoryStats> latestCategoryStats = new ArrayList<>();
    
    private StatisticsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);
        
        initViews(view);
        initViewModel();
        setupChart();
        observeViewModel();
        viewModel.refresh();
        
        return view;
    }

    private void initViews(View view) {
        todaySessionsText = view.findViewById(R.id.today_sessions);
        todayDurationText = view.findViewById(R.id.today_duration);
        weekSessionsText = view.findViewById(R.id.week_sessions);
        weekDurationText = view.findViewById(R.id.week_duration);
        monthSessionsText = view.findViewById(R.id.month_sessions);
        monthDurationText = view.findViewById(R.id.month_duration);
        chart = view.findViewById(R.id.chart);
        chartEmptyText = view.findViewById(R.id.chart_empty_text);
        chartEmptyImage = view.findViewById(R.id.chart_empty_image);
        monthlyChart = view.findViewById(R.id.monthly_chart);
        monthlyChartEmptyText = view.findViewById(R.id.monthly_chart_empty_text);
        hourlyDistributionChart = view.findViewById(R.id.hourly_distribution_chart);
        hourlyDistributionEmptyText = view.findViewById(R.id.hourly_distribution_empty_text);
        categoryPieChart = view.findViewById(R.id.category_pie_chart);
        categoryChartEmptyText = view.findViewById(R.id.category_chart_empty_text);
        pauseReasonChart = view.findViewById(R.id.pause_reason_chart);
        pauseReasonChartEmptyText = view.findViewById(R.id.pause_reason_chart_empty_text);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this,
                ((App) requireActivity().getApplication()).getContainer().getViewModelFactory())
                .get(StatisticsViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getTodayStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && todaySessionsText != null) {
                todaySessionsText.setText(getString(R.string.statistics_label_today_sessions, stats.count));
                todayDurationText.setText(formatDuration(stats.totalDuration));
            }
        });

        viewModel.getWeekStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && weekSessionsText != null) {
                weekSessionsText.setText(getString(R.string.statistics_label_week_sessions, stats.totalSessions));
                weekDurationText.setText(formatDuration(stats.totalDuration));
            }
        });

        viewModel.getMonthStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && monthSessionsText != null) {
                monthSessionsText.setText(getString(R.string.statistics_label_month_sessions, stats.totalSessions));
                monthDurationText.setText(formatDuration(stats.totalDuration));
            }
        });

        viewModel.getWeeklyChartData().observe(getViewLifecycleOwner(), this::updateChart);
        viewModel.getMonthlyChartData().observe(getViewLifecycleOwner(), this::updateMonthlyChart);
        viewModel.getHourlyChartData().observe(getViewLifecycleOwner(), this::updateHourlyDistributionChart);
        viewModel.getCategoryStats().observe(getViewLifecycleOwner(), this::updateCategoryPieChart);
        viewModel.getPauseReasonStats().observe(getViewLifecycleOwner(), this::updatePauseReasonChart);
    }

    private void loadStatistics() {
        viewModel.refresh();
    }

    private void setupChart() {
        // 设置7天图表
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setNoDataText(""); // 禁用默认的"No chart data available"消息
        
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new DateValueFormatter());
        
        // 设置Y轴（专注时间轴）
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setValueFormatter(new DurationValueFormatter());
        leftAxis.setAxisMinimum(0f); // 设置最小值为0，专注时间不能为负数
        leftAxis.setGranularity(5f); // 设置刻度间隔为5分钟
        leftAxis.setLabelCount(6, true); // 设置标签数量
        
        chart.getAxisRight().setEnabled(false);
        
        // 设置本月图表
        monthlyChart.getDescription().setEnabled(false);
        monthlyChart.setTouchEnabled(true);
        monthlyChart.setDragEnabled(true);
        monthlyChart.setScaleEnabled(true);
        monthlyChart.setPinchZoom(true);
        monthlyChart.setNoDataText(""); // 禁用默认的"No chart data available"消息
        
        XAxis monthlyXAxis = monthlyChart.getXAxis();
        monthlyXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        monthlyXAxis.setGranularity(5f); // 设置间隔为5天，避免标签重叠
        monthlyXAxis.setLabelCount(7, true); // 设置标签数量，显示约7个标签
        monthlyXAxis.setValueFormatter(new MonthlyDateValueFormatter());
        monthlyXAxis.setAxisMinimum(0f);
        monthlyXAxis.setAxisMaximum(30f); // 设置最大值为30（0-30对应1-31日）
        
        // 设置Y轴（专注时间轴）
        YAxis monthlyLeftAxis = monthlyChart.getAxisLeft();
        monthlyLeftAxis.setValueFormatter(new DurationValueFormatter());
        monthlyLeftAxis.setAxisMinimum(0f); // 设置最小值为0，专注时间不能为负数
        monthlyLeftAxis.setGranularity(10f); // 设置刻度间隔为10分钟
        monthlyLeftAxis.setLabelCount(6, true); // 设置标签数量
        
        monthlyChart.getAxisRight().setEnabled(false);
        
        // 设置时段分布图表
        hourlyDistributionChart.getDescription().setEnabled(false);
        hourlyDistributionChart.setTouchEnabled(true);
        hourlyDistributionChart.setDragEnabled(true);
        hourlyDistributionChart.setScaleEnabled(true);
        hourlyDistributionChart.setPinchZoom(true);
        hourlyDistributionChart.setNoDataText(""); // 禁用默认的"No chart data available"消息
        
        XAxis hourlyXAxis = hourlyDistributionChart.getXAxis();
        hourlyXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        hourlyXAxis.setGranularity(1f);
        hourlyXAxis.setValueFormatter(new HourValueFormatter());
        hourlyXAxis.setAxisMinimum(0f);
        hourlyXAxis.setAxisMaximum(24f);
        hourlyXAxis.setLabelCount(9, true); // 显示0,3,6,9,12,15,18,21,24点
        
        YAxis hourlyLeftAxis = hourlyDistributionChart.getAxisLeft();
        hourlyLeftAxis.setValueFormatter(new DurationValueFormatter());
        hourlyLeftAxis.setAxisMinimum(0f);
        hourlyLeftAxis.setGranularity(5f); // 设置刻度间隔为5分钟
        hourlyLeftAxis.setLabelCount(6, true);
        
        hourlyDistributionChart.getAxisRight().setEnabled(false);
    }

    private void updateChart(List<DailyStats> dailyStatsList) {
        List<Entry> entries = new ArrayList<>();
        
        // 调试日志
        AppLog.d("StatisticsFragment", "updateChart called with " + dailyStatsList.size() + " daily stats");
        for (DailyStats stats : dailyStatsList) {
            AppLog.d("StatisticsFragment", "DailyStats: date=" + stats.date + ", count=" + stats.count + ", duration=" + stats.totalDuration);
        }
        
        // 准备最近7天的数据
        Calendar calendar = Calendar.getInstance();
        float maxDuration = 0f; // 用于动态调整Y轴最大值
        boolean hasData = false; // 检查是否有任何数据
        
        for (int i = 6; i >= 0; i--) {
            Calendar dayCalendar = Calendar.getInstance();
            dayCalendar.add(Calendar.DAY_OF_MONTH, -i);
            String dateStr = new SimpleDateFormat("MM-dd", Locale.getDefault()).format(dayCalendar.getTime());
            String dbDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCalendar.getTime());
            
            AppLog.d("StatisticsFragment", "Processing day " + (6-i) + ": dateStr=" + dateStr + ", dbDateStr=" + dbDateStr);
            
            // 查找对应日期的数据
            long duration = 0;
            for (DailyStats stats : dailyStatsList) {
                // 数据库返回的是 yyyy-MM-dd 格式，需要匹配
                if (stats.date.equals(dbDateStr)) {
                    duration = stats.totalDuration;
                    AppLog.d("StatisticsFragment", "Found match for " + dbDateStr + ": duration=" + duration);
                    break;
                }
            }
            
            float durationMinutes = duration / (1000 * 60); // 转换为分钟
            AppLog.d("StatisticsFragment", "Final duration for " + dateStr + ": " + durationMinutes + " minutes");
            entries.add(new Entry(6 - i, durationMinutes));
            
            // 更新最大时长，用于Y轴范围调整
            if (durationMinutes > maxDuration) {
                maxDuration = durationMinutes;
            }
            
            // 检查是否有任何数据
            if (durationMinutes > 0) {
                hasData = true;
            }
        }
        
        // 如果没有数据，显示空状态提示
        if (!hasData) {
            chart.setVisibility(View.GONE);
            chartEmptyText.setVisibility(View.VISIBLE);
            return;
        }
        
        // 有数据时显示图表
        chart.setVisibility(View.VISIBLE);
        chartEmptyText.setVisibility(View.GONE);
        if (chartEmptyImage != null) chartEmptyImage.setVisibility(View.GONE);

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.statistics_chart_focus_minutes));
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleRadius(6f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(true); // 显示数值
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value);
            }
        });

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        
        // 动态设置Y轴最大值，确保图表显示合理
        YAxis leftAxis = chart.getAxisLeft();
        if (maxDuration > 0) {
            leftAxis.setAxisMaximum(maxDuration * 1.2f); // 最大值比实际最大值多20%
        } else {
            leftAxis.setAxisMaximum(60f); // 如果没有数据，默认最大值为60分钟
        }
        
        chart.invalidate();
    }

    private String formatDuration(long durationMs) {
        long minutes = durationMs / (1000 * 60);
        long hours = minutes / 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return getString(R.string.format_duration_hours_minutes, hours, minutes);
        }
        return getString(R.string.format_duration_minutes, minutes);
    }

    private class DateValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, (int) value - 6);
            return new SimpleDateFormat("MM/dd", Locale.getDefault()).format(calendar.getTime());
        }
    }

    private class DurationValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return getString(R.string.format_duration_minutes_float, value);
        }
    }

    private class MonthlyDateValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            int day = (int) value + 1;
            if (day % 5 == 0 || day == 1 || day == 31) {
                return getString(R.string.statistics_label_day, day);
            }
            return "";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
    }

    private void updateMonthlyChart(List<DailyStats> dailyStatsList) {
        List<Entry> entries = new ArrayList<>();
        
        // 调试日志
        AppLog.d("StatisticsFragment", "updateMonthlyChart called with " + dailyStatsList.size() + " daily stats");
        for (DailyStats stats : dailyStatsList) {
            AppLog.d("StatisticsFragment", "MonthlyDailyStats: date=" + stats.date + ", count=" + stats.count + ", duration=" + stats.totalDuration);
        }
        
        // 准备本月的数据
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        float maxDuration = 0f; // 用于动态调整Y轴最大值
        boolean hasData = false; // 检查是否有任何数据
        
        for (int day = 1; day <= daysInMonth; day++) {
            Calendar dayCalendar = Calendar.getInstance();
            dayCalendar.set(Calendar.DAY_OF_MONTH, day);
            String dbDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCalendar.getTime());
            
            AppLog.d("StatisticsFragment", "Processing monthly day " + day + ": dbDateStr=" + dbDateStr);
            
            // 查找对应日期的数据
            long duration = 0;
            for (DailyStats stats : dailyStatsList) {
                // 数据库返回的是 yyyy-MM-dd 格式，需要匹配
                if (stats.date.equals(dbDateStr)) {
                    duration = stats.totalDuration;
                    AppLog.d("StatisticsFragment", "Found monthly match for " + dbDateStr + ": duration=" + duration);
                    break;
                }
            }
            
            float durationMinutes = duration / (1000 * 60); // 转换为分钟
            AppLog.d("StatisticsFragment", "Final monthly duration for day " + day + ": " + durationMinutes + " minutes");
            entries.add(new Entry(day - 1, durationMinutes)); // 使用day-1作为X轴索引，因为Entry的索引从0开始
            
            // 更新最大时长，用于Y轴范围调整
            if (durationMinutes > maxDuration) {
                maxDuration = durationMinutes;
            }
            
            // 检查是否有任何数据
            if (durationMinutes > 0) {
                hasData = true;
            }
        }
        
        // 如果没有数据，显示空状态提示
        if (!hasData) {
            monthlyChart.setVisibility(View.GONE);
            monthlyChartEmptyText.setVisibility(View.VISIBLE);
            return;
        }
        
        // 有数据时显示图表
        monthlyChart.setVisibility(View.VISIBLE);
        monthlyChartEmptyText.setVisibility(View.GONE);

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.statistics_chart_focus_minutes));
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(true); // 显示数值
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value);
            }
        });

        LineData lineData = new LineData(dataSet);
        monthlyChart.setData(lineData);
        
        // 设置Y轴最大值
        if (maxDuration > 0) {
            monthlyChart.getAxisLeft().setAxisMaximum(maxDuration * 1.1f); // 留10%的余量
        }
        
        monthlyChart.invalidate(); // 刷新图表
    }

    private void updateHourlyDistributionChart(List<HourlyStats> hourlyStatsList) {
        List<com.github.mikephil.charting.data.BarEntry> entries = new ArrayList<>();
        
        // 调试日志
        AppLog.d("StatisticsFragment", "updateHourlyDistributionChart called with " + hourlyStatsList.size() + " hourly stats");
        for (HourlyStats stats : hourlyStatsList) {
            AppLog.d("StatisticsFragment", "HourlyStats: hour=" + stats.hour + ", duration=" + stats.totalDuration);
        }
        
        // 准备24小时的数据（0-23点）
        float maxDuration = 0f;
        boolean hasData = false;
        
        for (int hour = 0; hour < 24; hour++) {
            // 查找对应小时的数据
            long duration = 0;
            for (HourlyStats stats : hourlyStatsList) {
                if (stats.hour == hour) {
                    duration = stats.totalDuration;
                    AppLog.d("StatisticsFragment", "Found match for hour " + hour + ": duration=" + duration);
                    break;
                }
            }
            
            float durationMinutes = duration / (1000 * 60); // 转换为分钟
            AppLog.d("StatisticsFragment", "Final duration for hour " + hour + ": " + durationMinutes + " minutes");
            entries.add(new com.github.mikephil.charting.data.BarEntry(hour, durationMinutes));
            
            // 更新最大时长
            if (durationMinutes > maxDuration) {
                maxDuration = durationMinutes;
            }
            
            // 检查是否有任何数据
            if (durationMinutes > 0) {
                hasData = true;
            }
        }
        
        // 如果没有数据，显示空状态提示
        if (!hasData) {
            hourlyDistributionChart.setVisibility(View.GONE);
            hourlyDistributionEmptyText.setText(R.string.statistics_empty_hourly);
            hourlyDistributionEmptyText.setOnClickListener(null); // 确保没有点击监听器
            hourlyDistributionEmptyText.setVisibility(View.VISIBLE);
            return;
        }
        
        // 有数据时显示图表
        hourlyDistributionChart.setVisibility(View.VISIBLE);
        hourlyDistributionEmptyText.setVisibility(View.GONE);

        com.github.mikephil.charting.data.BarDataSet dataSet = new com.github.mikephil.charting.data.BarDataSet(entries, getString(R.string.statistics_chart_focus_minutes));
        dataSet.setColor(Color.parseColor("#FF9800")); // 橙色
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value);
            }
        });

        com.github.mikephil.charting.data.BarData barData = new com.github.mikephil.charting.data.BarData(dataSet);
        hourlyDistributionChart.setData(barData);
        
        // 设置Y轴最大值
        if (maxDuration > 0) {
            hourlyDistributionChart.getAxisLeft().setAxisMaximum(maxDuration * 1.2f); // 留20%的余量
        }
        
        hourlyDistributionChart.invalidate(); // 刷新图表
    }

    private void updatePauseReasonChart(List<PauseReasonStats> statsList) {
        if (pauseReasonChart == null || pauseReasonChartEmptyText == null) {
            return;
        }
        if (statsList == null || statsList.isEmpty()) {
            pauseReasonChart.setVisibility(View.GONE);
            pauseReasonChartEmptyText.setVisibility(View.VISIBLE);
            return;
        }

        pauseReasonChart.setVisibility(View.VISIBLE);
        pauseReasonChartEmptyText.setVisibility(View.GONE);

        pauseReasonChart.getDescription().setEnabled(false);
        pauseReasonChart.setFitBars(true);

        List<com.github.mikephil.charting.data.BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        float maxCount = 0f;
        for (int i = 0; i < statsList.size(); i++) {
            PauseReasonStats stats = statsList.get(i);
            float count = stats.count;
            entries.add(new com.github.mikephil.charting.data.BarEntry(i, count));
            labels.add(stats.pauseReason != null ? stats.pauseReason : getString(R.string.session_detail_none));
            if (count > maxCount) {
                maxCount = count;
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.statistics_pause_reason_chart_label));
        dataSet.setColor(Color.parseColor("#673AB7"));
        dataSet.setValueTextSize(11f);
        dataSet.setDrawValues(true);

        pauseReasonChart.setData(new BarData(dataSet));
        pauseReasonChart.getXAxis().setGranularity(1f);
        pauseReasonChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        pauseReasonChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    String label = labels.get(index);
                    return label.length() > 6 ? label.substring(0, 6) + "…" : label;
                }
                return "";
            }
        });
        if (maxCount > 0f) {
            pauseReasonChart.getAxisLeft().setAxisMaximum(maxCount * 1.2f);
        }
        pauseReasonChart.invalidate();
    }

    private void updateCategoryPieChart(List<CategoryStats> categoryStatsList) {
        if (categoryPieChart == null || categoryChartEmptyText == null) {
            return;
        }

        latestCategoryStats = categoryStatsList != null ? categoryStatsList : new ArrayList<>();
        if (latestCategoryStats.isEmpty()) {
            categoryPieChart.setVisibility(View.GONE);
            categoryChartEmptyText.setVisibility(View.VISIBLE);
            return;
        }

        categoryPieChart.setVisibility(View.VISIBLE);
        categoryChartEmptyText.setVisibility(View.GONE);

        List<PieEntry> entries = new ArrayList<>();
        for (CategoryStats stats : latestCategoryStats) {
            String label = stats.category != null && !stats.category.isEmpty()
                    ? stats.category : CategoryDefaults.getDefault();
            entries.add(new PieEntry(stats.totalDuration / (1000f * 60f), label));
        }

        int[] colors = {
                Color.parseColor("#4CAF50"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#F44336"),
                Color.parseColor("#009688"),
                Color.parseColor("#795548")
        };

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return getString(R.string.format_duration_minutes_float, value);
            }
        });

        PieData pieData = new PieData(dataSet);
        categoryPieChart.setData(pieData);
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.setDrawEntryLabels(true);
        categoryPieChart.setEntryLabelColor(Color.BLACK);
        categoryPieChart.setEntryLabelTextSize(11f);
        categoryPieChart.setUsePercentValues(false);
        categoryPieChart.setHighlightPerTapEnabled(true);
        categoryPieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    showCategoryDrillDown(((PieEntry) e).getLabel());
                }
            }

            @Override
            public void onNothingSelected() {
            }
        });
        categoryPieChart.invalidate();
    }

    private void showCategoryDrillDown(String category) {
        viewModel.loadCategoryDrillDown(category);
        viewModel.getCategoryDrillDownSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions == null || !isAdded()) {
                return;
            }
            long totalDuration = 0L;
            int count = 0;
            StringBuilder message = new StringBuilder();
            SimpleDateFormat timeFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            for (PomodoroSession session : sessions) {
                if (!session.completed) {
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
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.statistics_category_drilldown_title, category))
                    .setMessage(getString(R.string.statistics_category_drilldown_summary, count, formatDuration(totalDuration))
                            + "\n\n" + message)
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        });
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 当配置变化时（如深色模式切换），更新UI组件
        updateUIForThemeChange();
    }
    
    private void updateUIForThemeChange() {
        // 当主题变化时，更新UI组件的颜色和样式
        if (getView() == null) return;
        
        // 更新文字颜色
        if (todaySessionsText != null) {
            todaySessionsText.setTextColor(getResources().getColor(R.color.text_primary));
        }
        if (todayDurationText != null) {
            todayDurationText.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        if (weekSessionsText != null) {
            weekSessionsText.setTextColor(getResources().getColor(R.color.text_primary));
        }
        if (weekDurationText != null) {
            weekDurationText.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        if (monthSessionsText != null) {
            monthSessionsText.setTextColor(getResources().getColor(R.color.text_primary));
        }
        if (monthDurationText != null) {
            monthDurationText.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        if (chartEmptyText != null) {
            chartEmptyText.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        if (monthlyChartEmptyText != null) {
            monthlyChartEmptyText.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        if (hourlyDistributionEmptyText != null) {
            hourlyDistributionEmptyText.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        
        // 重新设置图表样式
        setupChart();
        loadStatistics();
    }

    private class HourValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            int hour = (int) value;
            if (hour == 0 || hour == 3 || hour == 6 || hour == 9 || hour == 12
                    || hour == 15 || hour == 18 || hour == 21 || hour == 24) {
                return getString(R.string.statistics_label_hour, hour);
            }
            return "";
        }
    }
}
