package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.skyinit.pomodorotimer.R;

/**
 * 根据背景色计算可读性更好的前景色（基于 WCAG 相对亮度与对比度）。
 */
public final class ColorContrastUtils {

    private ColorContrastUtils() {
    }

    public static int getContrastingTextColor(Context context, int backgroundColor) {
        int darkCandidate = ContextCompat.getColor(context, R.color.text_primary);
        int lightCandidate = ContextCompat.getColor(context, R.color.dark_text);

        double darkContrast = contrastRatio(backgroundColor, darkCandidate);
        double lightContrast = contrastRatio(backgroundColor, lightCandidate);

        return darkContrast >= lightContrast ? darkCandidate : lightCandidate;
    }

    public static double contrastRatio(int color1, int color2) {
        double l1 = relativeLuminance(color1);
        double l2 = relativeLuminance(color2);
        return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
    }

    private static double relativeLuminance(int color) {
        double r = linearize(Color.red(color) / 255.0);
        double g = linearize(Color.green(color) / 255.0);
        double b = linearize(Color.blue(color) / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearize(double channel) {
        return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
