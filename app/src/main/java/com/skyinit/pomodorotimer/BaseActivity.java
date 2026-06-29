package com.skyinit.pomodorotimer;

import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.util.ColorContrastUtils;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import com.skyinit.pomodorotimer.util.AppLog;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.xmlpull.v1.XmlPullParser;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected int currentThemeColor;
    protected int currentNightMode;
    private boolean isDarkModeEnabled = false;
    private static final int DEFAULT_LIGHT_THEME = R.color.default_theme;
    private static final int DEFAULT_DARK_THEME = R.color.default_theme_dark;
    // 预加载渐变资源缓存
    private static final SparseArray<GradientInfo> gradientCache = new SparseArray<>();

    // 检查是否使用默认主题
    private boolean isUsingDefaultTheme() {
        SettingsManager settings = new SettingsManager(this);
        int currentThemeRes = settings.getThemeColor();
        return currentThemeRes == DEFAULT_LIGHT_THEME ||
                currentThemeRes == DEFAULT_DARK_THEME;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyThemeStyle();
        super.onCreate(savedInstanceState);
        preloadGradientThemes();
        currentThemeColor = new SettingsManager(this).getThemeColor();
        currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    @Override
    protected void onStart() {
        super.onStart();
        applyTheme();
    }

    private void preloadGradientThemes() {
        int[] gradients = {
                R.drawable.themecolor21,
                R.drawable.themecolor22,
                R.drawable.themecolor23,
                R.drawable.themecolor24,
                R.drawable.themecolor25,
                R.drawable.themecolor26,
                R.drawable.themecolor27,
                R.drawable.themecolor28,
                R.drawable.themecolor29,
                R.drawable.themecolor30
        };

        for (int resId : gradients) {
            if (gradientCache.get(resId) == null) {
                gradientCache.put(resId, parseGradientDrawable(resId));
            }
        }
    }

    // 新增：先应用主题样式，确保主题资源生效
    protected void applyThemeStyle() {
        setTheme(R.style.Theme_PomodoroTimer);
    }

    public void applyTheme() {
        SettingsManager settings = new SettingsManager(this);
        int themeColor = settings.getThemeColor();

        try {
            // 检查是否使用默认主题，如果是则应用自适应背景
            if (isUsingDefaultTheme()) {
                AppLog.d(TAG, "Applying adaptive theme for default theme");
                applyAdaptiveTheme();
                return;
            }

            // 获取资源类型
            String resourceType = getResources().getResourceTypeName(themeColor);
            AppLog.d(TAG, "Applying theme: " + getResources().getResourceName(themeColor) + 
                  ", type: " + resourceType);

            if ("color".equals(resourceType)) {
                // 处理纯色主题
                AppLog.d(TAG, "Applying solid color theme");
                applySolidColorTheme(themeColor);
            } else if ("drawable".equals(resourceType)) {
                // 处理渐变色主题
                AppLog.d(TAG, "Applying gradient theme");
                applyGradientTheme(themeColor);
            }else {
                AppLog.w(TAG, "Unknown resource type: " + resourceType);
                applyDefaultTheme();
            }
        } catch (Resources.NotFoundException e) {
            AppLog.e("BaseActivity", "Theme resource not found", e);
            applyDefaultTheme();
        }
    }

    private int[] getGradientColors(int drawableRes) {
        int[] colors = new int[2];
        try {
            Drawable drawable = ContextCompat.getDrawable(this, drawableRes);
            if (drawable instanceof GradientDrawable) {
                GradientDrawable gradientDrawable = (GradientDrawable) drawable;
                int[] gradientColors = gradientDrawable.getColors();
                if (gradientColors != null && gradientColors.length >= 2) {
                    colors[0] = gradientColors[0];
                    colors[1] = gradientColors[gradientColors.length - 1];
                }
            }
        } catch (Exception e) {
            AppLog.e("BaseActivity", "Error extracting gradient colors", e);
        }
        return colors;
    }

    private void applySolidColorTheme(int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
            updateActionBarColor(color);
            onBarColorApplied(color);

        // 设置状态栏和导航栏颜色
        getWindow().setStatusBarColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(color);
        }

        // 计算颜色亮度
        double darkness = 1 - (0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)) / 255;

        // 设置系统栏图标颜色
        View decorView = getWindow().getDecorView();
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

        // 设置内容区域背景（半透明）
        View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            //使用半透明背景避免覆盖主题样式
            contentView.setBackgroundColor(
                    Color.argb(240, Color.red(color), Color.green(color), Color.blue(color))
            );
        }
    }

//

    private void applyGradientTheme(int drawableRes) {
        try {
            AppLog.d(TAG, "Starting gradient theme application for: " + getResources().getResourceName(drawableRes));
            
            // 解析渐变资源
            GradientInfo gradientInfo = gradientCache.get(drawableRes);
            if (gradientInfo == null) {
                AppLog.d(TAG, "Parsing gradient drawable for first time");
                gradientInfo = parseGradientDrawable(drawableRes);
                if (gradientInfo != null) {
                    gradientCache.put(drawableRes, gradientInfo);
                    AppLog.d(TAG, "Cached gradient info: startColor=" + 
                          String.format("#%08X", gradientInfo.startColor) + 
                          ", endColor=" + String.format("#%08X", gradientInfo.endColor));
                }
            } else {
                AppLog.d(TAG, "Using cached gradient info");
            }

            if (gradientInfo == null) {
                AppLog.w(TAG, "Failed to parse gradient theme, using default");
                applyDefaultTheme();
                return;
            }

            int barColor = getTopColorForGradient(gradientInfo);
            updateActionBarColor(barColor);
            onBarColorApplied(barColor);

            // 应用渐变起始色到状态栏
            getWindow().setStatusBarColor(barColor);
            AppLog.d(TAG, "Set status bar color: " + String.format("#%08X", barColor));

            // 应用渐变结束色到导航栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(gradientInfo.endColor);
                AppLog.d(TAG, "Set navigation bar color: " + String.format("#%08X", gradientInfo.endColor));
            }

            // 设置系统栏图标颜色
            setSystemBarIconColorBasedOnBackground(barColor);

            // 应用渐变背景
            View contentView = findViewById(android.R.id.content);
            if (contentView != null) {
                AppLog.d(TAG, "Setting background resource: " + getResources().getResourceName(drawableRes));
                // 直接设置渐变drawable
                contentView.setBackgroundResource(drawableRes);
                
                // 确保背景可见
                contentView.setVisibility(View.VISIBLE);
                
                // 强制刷新视图
                contentView.invalidate();
                AppLog.d(TAG, "Gradient background applied successfully");
            } else {
                AppLog.w(TAG, "Content view is null, cannot apply background");
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Error applying gradient theme", e);
            applyDefaultTheme();
        }
    }

    protected void onBarColorApplied(int barColor) {
        // 子类可覆盖以同步自定义 Toolbar
    }

    private void updateActionBarColor(int color) {
        ColorDrawable background = new ColorDrawable(color);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(background);
            getSupportActionBar().setElevation(4f);
        }
        View actionBarView = getWindow().getDecorView().findViewById(androidx.appcompat.R.id.action_bar);
        if (actionBarView != null) {
            actionBarView.setBackground(background);
        }
        applyActionBarTitleColor(color);
    }

    private void applyActionBarTitleColor(int barColor) {
        int titleColor = ColorContrastUtils.getContrastingTextColor(this, barColor);
        TextView titleView = findViewById(androidx.appcompat.R.id.action_bar_title);
        if (titleView != null) {
            titleView.setTextColor(titleColor);
        }
    }

    private void setSystemBarIconColorBasedOnBackground(int backgroundColor) {
        double darkness = 1 - (0.299 * Color.red(backgroundColor) +
                0.587 * Color.green(backgroundColor) +
                0.114 * Color.blue(backgroundColor)) / 255;

        View decorView = getWindow().getDecorView();
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

    private GradientInfo parseGradientDrawable(int drawableRes) {
        GradientInfo info = new GradientInfo();
        try {
            AppLog.d(TAG, "Parsing gradient drawable: " + getResources().getResourceName(drawableRes));
            Drawable drawable = ContextCompat.getDrawable(this, drawableRes);
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
            AppLog.e(TAG, "Error inflating gradient drawable", e);
        }

        if (info.startColor == 0 || info.endColor == 0) {
            parseGradientFromXml(drawableRes, info);
        }

        if (info.startColor == 0) {
            info.startColor = ContextCompat.getColor(this, R.color.default_theme);
            AppLog.d(TAG, "Using default startColor");
        }
        if (info.endColor == 0) {
            info.endColor = ContextCompat.getColor(this, R.color.default_theme_dark);
            AppLog.d(TAG, "Using default endColor");
        }

        AppLog.d(TAG, "Final gradient info: startColor=" + String.format("#%08X", info.startColor)
                + ", endColor=" + String.format("#%08X", info.endColor)
                + ", orientation=" + info.orientation);
        return info;
    }

    private void parseGradientFromXml(int drawableRes, GradientInfo info) {
        try {
            XmlResourceParser parser = getResources().getXml(drawableRes);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (eventType != XmlPullParser.START_TAG || !"gradient".equals(parser.getName())) {
                    continue;
                }
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String name = parser.getAttributeName(i);
                    String value = parser.getAttributeValue(i);
                    if ("startColor".equals(name)) {
                        info.startColor = resolveGradientColor(value, parser.getAttributeResourceValue(i, 0));
                    } else if ("endColor".equals(name)) {
                        info.endColor = resolveGradientColor(value, parser.getAttributeResourceValue(i, 0));
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

    private int resolveGradientColor(@Nullable String value, int colorRes) {
        if (colorRes != 0) {
            return ContextCompat.getColor(this, colorRes);
        }
        if (value != null) {
            return Color.parseColor(value);
        }
        return 0;
    }

    private GradientDrawable.Orientation angleToOrientation(int angle) {
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

    /**
     * 根据渐变方向取屏幕顶端对应的颜色，用于 ActionBar / 状态栏。
     */
    private int getTopColorForGradient(GradientInfo info) {
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

    private void applyAdaptiveTheme() {
        // 检测当前是否为深色模式
        boolean isDarkMode = isDarkMode();
        AppLog.d(TAG, "Dark mode detected: " + isDarkMode);
        
        // 根据深色模式选择背景颜色
        int backgroundColor = isDarkMode ? 
            ContextCompat.getColor(this, R.color.adaptive_background_dark) :
            ContextCompat.getColor(this, R.color.adaptive_background_light);
        
        AppLog.d(TAG, "Applying adaptive background color: " + 
              String.format("#%08X", backgroundColor));
        
        updateActionBarColor(backgroundColor);
        onBarColorApplied(backgroundColor);
        // 应用背景颜色到系统栏
        applyColorToSystemBars(backgroundColor);
        
        // 应用背景颜色到内容区域
        applySolidBackground(backgroundColor);
    }
    
    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & 
                           Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyDefaultTheme() {
        int defaultColor = ContextCompat.getColor(this, R.color.default_theme);
        updateActionBarColor(defaultColor);
        onBarColorApplied(defaultColor);
        applyColorToSystemBars(defaultColor);
        applySolidBackground(defaultColor);
    }

    private void applyColorToSystemBars(int color) {
        getWindow().setStatusBarColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(color);
        }
        setSystemBarIconColorBasedOnBackground(color);
    }

    private void applySolidBackground(int color) {
        View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.setBackgroundColor(
                    Color.argb(240, Color.red(color), Color.green(color), Color.blue(color))
            );
        }
    }

    // 在Activity可见时重新应用主题（确保切换后立即生效）
    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager settings = new SettingsManager(this);
        int latestThemeColor = settings.getThemeColor();
        int latestNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (latestThemeColor != currentThemeColor || latestNightMode != currentNightMode) {
            currentThemeColor = latestThemeColor;
            currentNightMode = latestNightMode;
            recreate();
            return;
        }
        applyTheme();
    }
    
    // 监听配置变化，特别是深色模式切换
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int latestNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (latestNightMode != currentNightMode) {
            currentNightMode = latestNightMode;
            recreate();
            return;
        }
        applyTheme();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清除背景引用防止内存泄漏
        View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.setBackground(null);
        }
    }

    // 渐变信息辅助类
    private static class GradientInfo {
        int startColor = 0;
        int endColor = 0;
        int angle = 270;
        GradientDrawable.Orientation orientation = GradientDrawable.Orientation.TOP_BOTTOM;
    }
}
