package com.skyinit.pomodorotimer.ui.statistics.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.skyinit.pomodorotimer.R;

import java.util.Arrays;
import java.util.Locale;

/**
 * 本月专注时间：可横滑视口 + 曲线到 X 轴的透明度渐变面积图。
 * 放在纵向 ScrollView 内时，水平拖动会抢占父级手势，避免误滚动。
 */
public class MonthlyFocusAreaChartView extends View {

    private static final int DEFAULT_VISIBLE_DAYS = 8;
    private static final long MARKER_AUTO_HIDE_MS = 2500L;

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final RectF contentRect = new RectF();
    private final OverScroller scroller;
    private final GestureDetector gestureDetector;

    /** index 0 = 1 日，单位：分钟。 */
    private float[] minutes = new float[0];
    private int visibleDayCount = DEFAULT_VISIBLE_DAYS;
    private float scrollXDays;
    private float maxScrollDays;
    private float maxMinutes = 60f;

    private int selectedIndex = -1;
    private long markerShownAt;
    private boolean draggingHorizontally;
    private float downX;
    private float downY;

    private int lineColor;
    private int labelColor;
    private int gridColor;
    private int markerBgColor;
    private int markerTextColor;

    private final Runnable hideMarkerRunnable = () -> {
        selectedIndex = -1;
        invalidate();
    };

    public MonthlyFocusAreaChartView(Context context) {
        this(context, null);
    }

    public MonthlyFocusAreaChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonthlyFocusAreaChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scroller = new OverScroller(context);
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                if (!scroller.isFinished()) {
                    scroller.forceFinished(true);
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                selectNearestDay(e.getX());
                return true;
            }

            @Override
            public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
                                   float distanceX, float distanceY) {
                if (minutes.length <= visibleDayCount) {
                    return false;
                }
                scrollByDays(distanceX / dayWidthPx());
                return true;
            }

            @Override
            public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
                                  float velocityX, float velocityY) {
                if (minutes.length <= visibleDayCount) {
                    return false;
                }
                float dayWidth = dayWidthPx();
                if (dayWidth <= 0f) {
                    return false;
                }
                int start = Math.round(scrollXDays * dayWidth);
                int max = Math.round(maxScrollDays * dayWidth);
                scroller.fling(start, 0, Math.round(-velocityX), 0, 0, max, 0, 0);
                ViewCompat.postInvalidateOnAnimation(MonthlyFocusAreaChartView.this);
                return true;
            }
        });
        gestureDetector.setIsLongpressEnabled(false);
        setClickable(true);
        setFocusable(true);
        applyThemeColors();
        configurePaints();
    }

    public void applyThemeColors() {
        Context context = getContext();
        lineColor = ContextCompat.getColor(context, R.color.primary);
        labelColor = ContextCompat.getColor(context, R.color.text_secondary);
        gridColor = ContextCompat.getColor(context, R.color.chart_grid);
        markerBgColor = ContextCompat.getColor(context, R.color.chart_marker_bg);
        markerTextColor = ContextCompat.getColor(context, R.color.chart_marker_text);
        configurePaints();
        invalidate();
    }

    private void configurePaints() {
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(dp(1f));
        axisPaint.setColor(gridColor);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1f));
        gridPaint.setColor(gridColor);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(lineColor);

        fillPaint.setStyle(Paint.Style.FILL);

        labelPaint.setTextSize(sp(10f));
        labelPaint.setColor(labelColor);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        markerBgPaint.setStyle(Paint.Style.FILL);
        markerBgPaint.setColor(markerBgColor);

        markerTextPaint.setColor(markerTextColor);
        markerTextPaint.setTextSize(sp(12f));
        markerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        markerTextPaint.setTextAlign(Paint.Align.CENTER);

        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(lineColor);

        pointRingPaint.setStyle(Paint.Style.STROKE);
        pointRingPaint.setStrokeWidth(dp(2f));
        pointRingPaint.setColor(lineColor);
    }

    /**
     * @param dayMinutes 长度 = 本月天数，index0 = 1 日
     * @param scrollToToday 是否滚到「今天」附近
     */
    public void setMonthData(@Nullable float[] dayMinutes, boolean scrollToToday) {
        removeCallbacks(hideMarkerRunnable);
        selectedIndex = -1;
        if (dayMinutes == null || dayMinutes.length == 0) {
            minutes = new float[0];
            scrollXDays = 0f;
            maxScrollDays = 0f;
            maxMinutes = 60f;
            invalidate();
            return;
        }
        minutes = Arrays.copyOf(dayMinutes, dayMinutes.length);
        visibleDayCount = Math.min(DEFAULT_VISIBLE_DAYS, minutes.length);
        maxScrollDays = Math.max(0f, minutes.length - visibleDayCount);
        float peak = 0f;
        for (float value : minutes) {
            if (value > peak) {
                peak = value;
            }
        }
        maxMinutes = peak > 0f ? peak * 1.25f : 60f;
        if (scrollToToday) {
            int todayIndex = Math.min(minutes.length - 1,
                    Math.max(0, java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH) - 1));
            scrollXDays = clamp(todayIndex - visibleDayCount + 1.5f, 0f, maxScrollDays);
        } else {
            scrollXDays = clamp(scrollXDays, 0f, maxScrollDays);
        }
        invalidate();
    }

    public boolean hasData() {
        for (float value : minutes) {
            if (value > 0f) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (minutes.length == 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        float leftPad = dp(36f);
        float rightPad = dp(12f);
        float topPad = dp(28f);
        float bottomPad = dp(28f);
        contentRect.set(leftPad, topPad, getWidth() - rightPad, getHeight() - bottomPad);
        if (contentRect.width() <= 0f || contentRect.height() <= 0f) {
            return;
        }

        drawGridAndAxis(canvas);
        buildPaths();
        int fillAlphaTop = 0x99;
        fillPaint.setShader(new LinearGradient(
                0f, contentRect.top,
                0f, contentRect.bottom,
                (lineColor & 0x00FFFFFF) | (fillAlphaTop << 24),
                lineColor & 0x00FFFFFF,
                Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);
        fillPaint.setShader(null);
        canvas.drawPath(linePath, linePaint);
        drawXLabels(canvas);
        if (selectedIndex >= 0 && selectedIndex < minutes.length) {
            drawMarker(canvas, selectedIndex);
            if (SystemClock.uptimeMillis() - markerShownAt > MARKER_AUTO_HIDE_MS) {
                selectedIndex = -1;
            }
        }
    }

    private void drawGridAndAxis(@NonNull Canvas canvas) {
        int lines = 4;
        for (int i = 0; i <= lines; i++) {
            float y = contentRect.bottom - (contentRect.height() * i / lines);
            canvas.drawLine(contentRect.left, y, contentRect.right, y, gridPaint);
            float value = maxMinutes * i / lines;
            String label = String.format(Locale.getDefault(), "%.0f", value);
            labelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(label, contentRect.left - dp(6f), y + sp(3.5f), labelPaint);
        }
        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawLine(contentRect.left, contentRect.bottom, contentRect.right, contentRect.bottom, axisPaint);
    }

    private void buildPaths() {
        linePath.reset();
        fillPath.reset();
        int count = minutes.length;
        if (count == 0) {
            return;
        }
        float firstX = xForIndex(0);
        float firstY = yForValue(minutes[0]);
        linePath.moveTo(firstX, firstY);
        fillPath.moveTo(firstX, contentRect.bottom);
        fillPath.lineTo(firstX, firstY);

        for (int i = 0; i < count - 1; i++) {
            float x0 = xForIndex(i);
            float y0 = yForValue(minutes[i]);
            float x1 = xForIndex(i + 1);
            float y1 = yForValue(minutes[i + 1]);
            float cx = (x0 + x1) / 2f;
            linePath.cubicTo(cx, y0, cx, y1, x1, y1);
            fillPath.cubicTo(cx, y0, cx, y1, x1, y1);
        }
        fillPath.lineTo(xForIndex(count - 1), contentRect.bottom);
        fillPath.close();
    }

    private void drawXLabels(@NonNull Canvas canvas) {
        int start = Math.max(0, (int) Math.floor(scrollXDays));
        int end = Math.min(minutes.length - 1, (int) Math.ceil(scrollXDays + visibleDayCount));
        int step = visibleDayCount <= 8 ? 1 : 2;
        for (int i = start; i <= end; i++) {
            if (i % step != 0 && i != minutes.length - 1) {
                continue;
            }
            float x = xForIndex(i);
            if (x < contentRect.left - dp(8f) || x > contentRect.right + dp(8f)) {
                continue;
            }
            canvas.drawText(String.valueOf(i + 1), x, contentRect.bottom + dp(16f), labelPaint);
        }
    }

    private void drawMarker(@NonNull Canvas canvas, int index) {
        float x = xForIndex(index);
        float y = yForValue(minutes[index]);
        if (x < contentRect.left || x > contentRect.right) {
            return;
        }
        canvas.drawLine(x, contentRect.top, x, contentRect.bottom, gridPaint);
        canvas.drawCircle(x, y, dp(5f), pointPaint);
        canvas.drawCircle(x, y, dp(8f), pointRingPaint);

        String text = getResources().getString(R.string.statistics_month_marker,
                index + 1, Math.round(minutes[index]));
        float paddingH = dp(10f);
        float paddingV = dp(6f);
        float textWidth = markerTextPaint.measureText(text);
        float boxWidth = textWidth + paddingH * 2f;
        float boxHeight = sp(12f) + paddingV * 2f;
        float left = clamp(x - boxWidth / 2f, contentRect.left, contentRect.right - boxWidth);
        float top = Math.max(dp(4f), y - boxHeight - dp(12f));
        RectF box = new RectF(left, top, left + boxWidth, top + boxHeight);
        canvas.drawRoundRect(box, dp(8f), dp(8f), markerBgPaint);
        canvas.drawText(text, box.centerX(), box.centerY() + sp(4f), markerTextPaint);
    }

    private float xForIndex(int index) {
        float dayWidth = dayWidthPx();
        return contentRect.left + (index - scrollXDays) * dayWidth;
    }

    private float yForValue(float minutesValue) {
        float ratio = maxMinutes <= 0f ? 0f : Math.min(1f, Math.max(0f, minutesValue / maxMinutes));
        return contentRect.bottom - ratio * contentRect.height();
    }

    private float dayWidthPx() {
        if (visibleDayCount <= 1) {
            return contentRect.width();
        }
        return contentRect.width() / (visibleDayCount - 1f);
    }

    private void selectNearestDay(float touchX) {
        if (minutes.length == 0 || dayWidthPx() <= 0f) {
            return;
        }
        float relative = (touchX - contentRect.left) / dayWidthPx() + scrollXDays;
        int index = Math.round(relative);
        index = Math.max(0, Math.min(minutes.length - 1, index));
        selectedIndex = index;
        markerShownAt = SystemClock.uptimeMillis();
        removeCallbacks(hideMarkerRunnable);
        postDelayed(hideMarkerRunnable, MARKER_AUTO_HIDE_MS);
        invalidate();
    }

    private void scrollByDays(float deltaDays) {
        scrollXDays = clamp(scrollXDays + deltaDays, 0f, maxScrollDays);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            float dayWidth = dayWidthPx();
            if (dayWidth > 0f) {
                scrollXDays = clamp(scroller.getCurrX() / dayWidth, 0f, maxScrollDays);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (minutes.length == 0) {
            return super.onTouchEvent(event);
        }
        boolean handled = gestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                draggingHorizontally = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                if (!draggingHorizontally && dx > dy && dx > dp(6f)) {
                    draggingHorizontally = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else if (!draggingHorizontally && dy > dx && dy > dp(6f)) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingHorizontally = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            default:
                break;
        }
        return handled || super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(hideMarkerRunnable);
        super.onDetachedFromWindow();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
