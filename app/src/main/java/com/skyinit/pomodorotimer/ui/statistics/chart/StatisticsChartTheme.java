package com.skyinit.pomodorotimer.ui.statistics.chart;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.CategoryDefaults;

/**
 * 统计页 MPAndroidChart 统一主题，避免 Fragment 内散落硬编码色值。
 */
public final class StatisticsChartTheme {

    private StatisticsChartTheme() {
    }

    @ColorInt
    public static int primary(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.brand);
    }

    @ColorInt
    public static int axisText(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.text_secondary);
    }

    @ColorInt
    public static int grid(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.chart_grid);
    }

    @ColorInt
    public static int weekBar(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.chart_week_bar);
    }

    @ColorInt
    public static int hourlyBar(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.chart_hourly_bar);
    }

    @ColorInt
    public static int pauseBar(@NonNull Context context) {
        return ContextCompat.getColor(context, R.color.chart_pause_bar);
    }

    @ColorInt
    public static int categorySlice(@NonNull Context context, @NonNull String category) {
        if (CategoryDefaults.getDefault().equals(category)) {
            return ContextCompat.getColor(context, R.color.task_cat_default_border);
        }
        if (CategoryDefaults.getWork().equals(category)) {
            return ContextCompat.getColor(context, R.color.task_cat_work_border);
        }
        if (CategoryDefaults.getStudy().equals(category)) {
            return ContextCompat.getColor(context, R.color.task_cat_study_border);
        }
        if (CategoryDefaults.getLife().equals(category)) {
            return ContextCompat.getColor(context, R.color.task_cat_life_border);
        }
        if (CategoryDefaults.getSports().equals(category)) {
            return ContextCompat.getColor(context, R.color.task_cat_sports_border);
        }
        if (CategoryDefaults.getEntertainment().equals(category)) {
            return ContextCompat.getColor(context, R.color.task_cat_entertainment_border);
        }
        return ContextCompat.getColor(context, R.color.task_cat_other_border);
    }

    public static void applyCartesianChrome(@NonNull Chart<?> chart, @NonNull Context context) {
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("");
        chart.setExtraOffsets(8f, 8f, 8f, 8f);
        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        if (chart instanceof BarChart) {
            BarChart barChart = (BarChart) chart;
            styleXAxis(barChart.getXAxis(), context);
            styleYAxis(barChart.getAxisLeft(), context);
            barChart.getAxisRight().setEnabled(false);
            barChart.setDrawGridBackground(false);
            barChart.setDrawBorders(false);
            barChart.setScaleEnabled(false);
            barChart.setPinchZoom(false);
            barChart.setDoubleTapToZoomEnabled(false);
            barChart.setHighlightPerDragEnabled(false);
        }
    }

    public static void applyPieChrome(@NonNull PieChart pieChart, @NonNull Context context) {
        pieChart.getDescription().setEnabled(false);
        pieChart.setNoDataText("");
        pieChart.setDrawEntryLabels(false);
        pieChart.setUsePercentValues(false);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(62f);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleColor(Color.TRANSPARENT);
        pieChart.setRotationEnabled(false);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.setExtraOffsets(12f, 8f, 12f, 8f);
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setTextColor(axisText(context));
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(8f);
        legend.setTextSize(11f);
    }

    private static void styleXAxis(@NonNull XAxis axis, @NonNull Context context) {
        axis.setPosition(XAxis.XAxisPosition.BOTTOM);
        axis.setDrawGridLines(false);
        axis.setDrawAxisLine(false);
        axis.setTextColor(axisText(context));
        axis.setTextSize(10f);
        axis.setYOffset(4f);
    }

    private static void styleYAxis(@NonNull YAxis axis, @NonNull Context context) {
        axis.setDrawAxisLine(false);
        axis.setGridColor(grid(context));
        axis.setTextColor(axisText(context));
        axis.setTextSize(10f);
        axis.setAxisMinimum(0f);
        axis.setXOffset(4f);
    }
}
