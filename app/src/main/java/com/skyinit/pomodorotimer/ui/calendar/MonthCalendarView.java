package com.skyinit.pomodorotimer.ui.calendar;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.skyinit.pomodorotimer.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 轻量月历视图。日期标记由 {@link CalendarDayCellView} 以固定半径绘制，避免背景被拉伸。
 */
public class MonthCalendarView extends LinearLayout {

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar date);
    }

    public interface OnMonthChangedListener {
        void onMonthChanged(int year, int month);
    }

    private static final int DAY_CELL_HEIGHT_DP = 44;
    private static final int NAV_BUTTON_SIZE_DP = 40;

    private final TextView monthTitleView;
    private final GridLayout weekHeader;
    private final GridLayout daysGrid;
    private final Calendar displayedMonth = Calendar.getInstance();
    private Calendar selectedDate = Calendar.getInstance();
    private Set<String> highlightedDates = new HashSet<>();
    private OnDateSelectedListener dateSelectedListener;
    private OnMonthChangedListener monthChangedListener;

    public MonthCalendarView(Context context) {
        this(context, null);
    }

    public MonthCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams headerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        header.setLayoutParams(headerParams);
        header.setPadding(dp(4), dp(2), dp(4), dp(6));

        ImageButton prevButton = createNavButton(context, R.drawable.ic_chevron_left,
                context.getString(R.string.calendar_prev_month));
        prevButton.setOnClickListener(v -> shiftMonth(-1));

        monthTitleView = new TextView(context);
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        monthTitleView.setLayoutParams(titleParams);
        monthTitleView.setGravity(Gravity.CENTER);
        monthTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        monthTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        monthTitleView.setTextColor(ContextCompat.getColor(context, R.color.text_primary));

        ImageButton nextButton = createNavButton(context, R.drawable.ic_chevron_right,
                context.getString(R.string.calendar_next_month));
        nextButton.setOnClickListener(v -> shiftMonth(1));

        header.addView(prevButton);
        header.addView(monthTitleView);
        header.addView(nextButton);
        addView(header);

        weekHeader = new GridLayout(context);
        weekHeader.setColumnCount(7);
        weekHeader.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        int cellPadding = dp(4);
        String[] weekLabels = context.getResources().getStringArray(R.array.week_day_short_labels);
        for (String label : weekLabels) {
            TextView tv = createWeekHeaderCell(label);
            tv.setPadding(cellPadding, cellPadding, cellPadding, cellPadding);
            tv.setLayoutParams(createGridCellParams(dp(28)));
            weekHeader.addView(tv);
        }
        addView(weekHeader);

        daysGrid = new GridLayout(context);
        daysGrid.setColumnCount(7);
        daysGrid.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(daysGrid);

        renderMonth();
    }

    /** 主题切换后刷新标题与星期栏颜色。 */
    public void refreshTheme() {
        monthTitleView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        for (int i = 0; i < weekHeader.getChildCount(); i++) {
            View child = weekHeader.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(
                        ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
        }
        renderMonth();
    }

    private ImageButton createNavButton(Context context, int iconRes, String description) {
        ImageButton button = new ImageButton(context);
        int size = dp(NAV_BUTTON_SIZE_DP);
        LayoutParams params = new LayoutParams(size, size);
        button.setLayoutParams(params);
        button.setImageResource(iconRes);
        button.setContentDescription(description);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        button.setBackgroundResource(outValue.resourceId);
        button.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
        return button;
    }

    public void setHighlightedDates(Set<String> dates) {
        highlightedDates = dates != null ? dates : new HashSet<>();
        renderMonth();
    }

    public void setSelectedDate(Calendar date) {
        if (date == null) {
            return;
        }
        selectedDate = (Calendar) date.clone();
        displayedMonth.setTime(date.getTime());
        renderMonth();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.dateSelectedListener = listener;
    }

    public void setOnMonthChangedListener(OnMonthChangedListener listener) {
        this.monthChangedListener = listener;
    }

    private void shiftMonth(int delta) {
        displayedMonth.add(Calendar.MONTH, delta);
        renderMonth();
        if (monthChangedListener != null) {
            monthChangedListener.onMonthChanged(
                    displayedMonth.get(Calendar.YEAR),
                    displayedMonth.get(Calendar.MONTH));
        }
    }

    private void renderMonth() {
        SimpleDateFormat titleFormat = new SimpleDateFormat(
                getContext().getString(R.string.format_date_year_month), Locale.getDefault());
        monthTitleView.setText(titleFormat.format(displayedMonth.getTime()));

        daysGrid.removeAllViews();
        Calendar cursor = (Calendar) displayedMonth.clone();
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        // Calendar: Sunday=1 … Saturday=7 → Monday-first column index 0…6
        int firstDayOfWeek = (cursor.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = cursor.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            View spacer = new View(getContext());
            spacer.setLayoutParams(createGridCellParams(dp(DAY_CELL_HEIGHT_DP)));
            daysGrid.addView(spacer);
        }

        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar today = Calendar.getInstance();

        for (int day = 1; day <= daysInMonth; day++) {
            Calendar dayCal = (Calendar) displayedMonth.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, day);
            String key = keyFormat.format(dayCal.getTime());
            boolean hasRecord = highlightedDates.contains(key);
            boolean selected = isSameDay(dayCal, selectedDate);
            boolean isToday = isSameDay(dayCal, today);

            CalendarDayCellView cell = new CalendarDayCellView(getContext());
            cell.bind(day, hasRecord, selected, isToday);
            cell.setLayoutParams(createGridCellParams(dp(DAY_CELL_HEIGHT_DP)));
            cell.setContentDescription(buildContentDescription(day, hasRecord, selected, isToday));
            cell.setClickable(true);
            cell.setFocusable(true);
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            cell.setBackgroundResource(outValue.resourceId);
            cell.setOnClickListener(v -> {
                selectedDate = dayCal;
                if (dateSelectedListener != null) {
                    dateSelectedListener.onDateSelected((Calendar) dayCal.clone());
                }
                renderMonth();
            });
            daysGrid.addView(cell);
        }
    }

    private GridLayout.LayoutParams createGridCellParams(int heightPx) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = heightPx;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return lp;
    }

    private TextView createWeekHeaderCell(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private String buildContentDescription(int day, boolean hasRecord, boolean selected, boolean isToday) {
        StringBuilder builder = new StringBuilder();
        builder.append(getContext().getString(R.string.calendar_a11y_day_suffix, day));
        if (isToday) {
            builder.append(getContext().getString(R.string.calendar_a11y_today));
        }
        if (hasRecord) {
            builder.append(getContext().getString(R.string.calendar_a11y_has_records));
        }
        if (selected) {
            builder.append(getContext().getString(R.string.calendar_a11y_selected));
        }
        return builder.toString();
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
