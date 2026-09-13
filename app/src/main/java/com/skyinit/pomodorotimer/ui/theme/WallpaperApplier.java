package com.skyinit.pomodorotimer.ui.theme;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.ColorContrastUtils;

import org.xmlpull.v1.XmlPullParser;

/**
 * Applies wallpaper skins to window content + system bars only.
 * Does not mutate brand / component color tokens.
 */
public final class WallpaperApplier {

    private static final String TAG = "WallpaperApplier";
    private static final SparseArray<GradientInfo> GRADIENT_CACHE = new SparseArray<>();

    public interface BarColorListener {
        void onBarColorApplied(int barColor);
    }

    private WallpaperApplier() {
    }

    public static void preloadGradients(@NonNull Activity activity) {
        int[] gradients = {
                R.drawable.wallpaper_gradient_01, R.drawable.wallpaper_gradient_02,
                R.drawable.wallpaper_gradient_03, R.drawable.wallpaper_gradient_04,
                R.drawable.wallpaper_gradient_05, R.drawable.wallpaper_gradient_06,
                R.drawable.wallpaper_gradient_07, R.drawable.wallpaper_gradient_08,
                R.drawable.wallpaper_gradient_09, R.drawable.wallpaper_gradient_10
        };
        for (int resId : gradients) {
            if (GRADIENT_CACHE.get(resId) == null) {
                GRADIENT_CACHE.put(resId, parseGradientDrawable(activity, resId));
            }
        }
    }

    public static void apply(@NonNull Activity activity,
                             @NonNull WallpaperCatalog.WallpaperOption option,
                             @Nullable BarColorListener listener) {
        try {
            if (WallpaperCatalog.isDefaultKey(option.key)) {
                applyDefaultSurface(activity, listener);
                return;
            }
            String type = activity.getResources().getResourceTypeName(option.resId);
            if ("color".equals(type)) {
                applySolid(activity, ContextCompat.getColor(activity, option.resId), listener);
            } else if ("drawable".equals(type)) {
                applyGradient(activity, option.resId, listener);
            } else {
                AppLog.w(TAG, "Unknown wallpaper resource type: " + type);
                applyDefaultSurface(activity, listener);
            }
        } catch (Resources.NotFoundException e) {
            AppLog.e(TAG, "Wallpaper resource missing", e);
            applyDefaultSurface(activity, listener);
        }
    }

    private static void applyDefaultSurface(@NonNull Activity activity,
                                            @Nullable BarColorListener listener) {
        int backgroundColor = ContextCompat.getColor(activity, R.color.surface_page);
        applySolid(activity, backgroundColor, listener);
    }

    private static void applySolid(@NonNull Activity activity,
                                   int color,
                                   @Nullable BarColorListener listener) {
        updateActionBarColor(activity, color);
        if (listener != null) {
            listener.onBarColorApplied(color);
        }
        applySystemBars(activity, color);
        View contentView = activity.findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.setBackgroundColor(
                    Color.argb(240, Color.red(color), Color.green(color), Color.blue(color)));
        }
    }

    private static void applyGradient(@NonNull Activity activity,
                                      int drawableRes,
                                      @Nullable BarColorListener listener) {
        try {
            GradientInfo info = GRADIENT_CACHE.get(drawableRes);
            if (info == null) {
                info = parseGradientDrawable(activity, drawableRes);
                GRADIENT_CACHE.put(drawableRes, info);
            }
            if (info == null) {
                applyDefaultSurface(activity, listener);
                return;
            }
            int barColor = getTopColorForGradient(info);
            updateActionBarColor(activity, barColor);
            if (listener != null) {
                listener.onBarColorApplied(barColor);
            }
            activity.getWindow().setStatusBarColor(barColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setNavigationBarColor(info.endColor);
            }
            setSystemBarIconColor(activity, barColor);
            View contentView = activity.findViewById(android.R.id.content);
            if (contentView != null) {
                contentView.setBackgroundResource(drawableRes);
                contentView.setVisibility(View.VISIBLE);
                contentView.invalidate();
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Gradient wallpaper failed", e);
            applyDefaultSurface(activity, listener);
        }
    }

    private static void applySystemBars(@NonNull Activity activity, int color) {
        activity.getWindow().setStatusBarColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setNavigationBarColor(color);
        }
        setSystemBarIconColor(activity, color);
    }

    private static void setSystemBarIconColor(@NonNull Activity activity, int backgroundColor) {
        double darkness = 1 - (0.299 * Color.red(backgroundColor)
                + 0.587 * Color.green(backgroundColor)
                + 0.114 * Color.blue(backgroundColor)) / 255;
        View decorView = activity.getWindow().getDecorView();
        int flags = decorView.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = darkness < 0.5
                    ? flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    : flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = darkness < 0.5
                    ? flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    : flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decorView.setSystemUiVisibility(flags);
    }

    private static void updateActionBarColor(@NonNull Activity activity, int color) {
        if (activity.findViewById(R.id.subpage_toolbar) != null) {
            return;
        }
        ColorDrawable background = new ColorDrawable(color);
        if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.AppCompatActivity appCompat =
                    (androidx.appcompat.app.AppCompatActivity) activity;
            if (appCompat.getSupportActionBar() != null) {
                appCompat.getSupportActionBar().setBackgroundDrawable(background);
                appCompat.getSupportActionBar().setElevation(4f);
            }
        }
        View actionBarView = activity.getWindow().getDecorView()
                .findViewById(androidx.appcompat.R.id.action_bar);
        if (actionBarView != null) {
            actionBarView.setBackground(background);
        }
        int titleColor = ColorContrastUtils.getContrastingTextColor(activity, color);
        TextView titleView = activity.findViewById(androidx.appcompat.R.id.action_bar_title);
        if (titleView != null) {
            titleView.setTextColor(titleColor);
        }
    }

    @Nullable
    private static GradientInfo parseGradientDrawable(@NonNull Activity activity, int drawableRes) {
        GradientInfo info = new GradientInfo();
        try {
            Drawable drawable = ContextCompat.getDrawable(activity, drawableRes);
            if (drawable instanceof GradientDrawable) {
                GradientDrawable gradientDrawable = (GradientDrawable) drawable;
                int[] colors = gradientDrawable.getColors();
                if (colors != null && colors.length > 0) {
                    info.startColor = colors[0];
                    info.endColor = colors[colors.length - 1];
                }
                GradientDrawable.Orientation orientation = gradientDrawable.getOrientation();
                if (orientation != null) {
                    info.orientation = orientation;
                }
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Error inflating gradient", e);
        }
        if (info.startColor == 0 || info.endColor == 0) {
            parseGradientFromXml(activity, drawableRes, info);
        }
        if (info.startColor == 0) {
            info.startColor = ContextCompat.getColor(activity, R.color.surface_page);
        }
        if (info.endColor == 0) {
            info.endColor = ContextCompat.getColor(activity, R.color.surface_page);
        }
        return info;
    }

    private static void parseGradientFromXml(@NonNull Activity activity,
                                             int drawableRes,
                                             @NonNull GradientInfo info) {
        try {
            XmlResourceParser parser = activity.getResources().getXml(drawableRes);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (eventType != XmlPullParser.START_TAG || !"gradient".equals(parser.getName())) {
                    continue;
                }
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String name = parser.getAttributeName(i);
                    String value = parser.getAttributeValue(i);
                    if ("startColor".equals(name)) {
                        info.startColor = resolveColor(activity, value, parser.getAttributeResourceValue(i, 0));
                    } else if ("endColor".equals(name)) {
                        info.endColor = resolveColor(activity, value, parser.getAttributeResourceValue(i, 0));
                    } else if ("angle".equals(name) && value != null) {
                        info.angle = Integer.parseInt(value);
                        info.orientation = angleToOrientation(info.angle);
                    }
                }
                break;
            }
            parser.close();
        } catch (Exception e) {
            AppLog.e(TAG, "Error parsing gradient XML", e);
        }
    }

    private static int resolveColor(@NonNull Activity activity, @Nullable String value, int colorRes) {
        if (colorRes != 0) {
            return ContextCompat.getColor(activity, colorRes);
        }
        if (value != null) {
            return Color.parseColor(value);
        }
        return 0;
    }

    private static GradientDrawable.Orientation angleToOrientation(int angle) {
        switch (angle) {
            case 45:
                return GradientDrawable.Orientation.BL_TR;
            case 90:
                return GradientDrawable.Orientation.BOTTOM_TOP;
            case 135:
                return GradientDrawable.Orientation.BR_TL;
            case 180:
                return GradientDrawable.Orientation.RIGHT_LEFT;
            case 225:
                return GradientDrawable.Orientation.TL_BR;
            case 315:
                return GradientDrawable.Orientation.TR_BL;
            case 0:
                return GradientDrawable.Orientation.LEFT_RIGHT;
            case 270:
            default:
                return GradientDrawable.Orientation.TOP_BOTTOM;
        }
    }

    private static int getTopColorForGradient(@NonNull GradientInfo info) {
        GradientDrawable.Orientation orientation = info.orientation != null
                ? info.orientation
                : angleToOrientation(info.angle);
        switch (orientation) {
            case BOTTOM_TOP:
            case TR_BL:
            case RIGHT_LEFT:
            case BR_TL:
                return info.endColor;
            case TOP_BOTTOM:
            case TL_BR:
            case LEFT_RIGHT:
            case BL_TR:
            default:
                return info.startColor;
        }
    }

    public static boolean isDarkMode(@NonNull Activity activity) {
        int nightModeFlags = activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private static final class GradientInfo {
        int startColor = 0;
        int endColor = 0;
        int angle = 270;
        GradientDrawable.Orientation orientation = GradientDrawable.Orientation.TOP_BOTTOM;
    }
}
