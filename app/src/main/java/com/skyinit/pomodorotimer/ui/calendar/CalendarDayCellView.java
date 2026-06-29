package com.skyinit.pomodorotimer.ui.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.skyinit.pomodorotimer.R;

/**
 * 日历单日单元格：在固定半径内绘制圆环/实心圆，避免背景 drawable 被 Grid 拉伸。
 * <ul>
 *   <li>有记录且未选中：细圆环</li>
 *   <li>选中：小实心圆</li>
 *   <li>今天且未选中：仅文字主题色</li>
 * </ul>
 */
public class CalendarDayCellView extends View {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String dayLabel = "";
    private boolean hasRecord;
    private boolean selected;
    private boolean isToday;

    private float circleRadiusPx;
    private float strokeWidthPx;
    private float textSizePx;

    public CalendarDayCellView(Context context) {
        this(context, null);
    }

    public CalendarDayCellView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        circleRadiusPx = dp(13f);
        strokeWidthPx = dp(1.5f);
        textSizePx = sp(15f);

        fillPaint.setStyle(Paint.Style.FILL);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(strokeWidthPx);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSizePx);
    }

    public void bind(int day, boolean hasRecord, boolean selected, boolean isToday) {
        this.dayLabel = String.valueOf(day);
        this.hasRecord = hasRecord;
        this.selected = selected;
        this.isToday = isToday;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dayLabel.isEmpty()) {
            return;
        }

        float cx = getWidth() * 0.5f;
        float cy = getHeight() * 0.5f;

        int primary = ContextCompat.getColor(getContext(), R.color.primary);
        int textPrimary = ContextCompat.getColor(getContext(), R.color.text_primary);
        int onPrimary = ContextCompat.getColor(getContext(), R.color.white);

        if (selected) {
            fillPaint.setColor(primary);
            canvas.drawCircle(cx, cy, circleRadiusPx, fillPaint);
            textPaint.setColor(onPrimary);
            textPaint.setFakeBoldText(true);
        } else {
            textPaint.setFakeBoldText(isToday);
            if (hasRecord) {
                ringPaint.setColor(withAlphaFraction(primary, 0.45f));
                canvas.drawCircle(cx, cy, circleRadiusPx, ringPaint);
            }
            textPaint.setColor(isToday ? primary : textPrimary);
        }

        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float textY = cy - (metrics.ascent + metrics.descent) * 0.5f;
        canvas.drawText(dayLabel, cx, textY, textPaint);
    }

    private static int withAlphaFraction(int color, float alphaFraction) {
        int alpha = Math.round(255f * Math.max(0f, Math.min(alphaFraction, 1f)));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics());
    }
}
