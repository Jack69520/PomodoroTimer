package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ThemeUtils {
    public static void applyTheme(AppCompatActivity activity, int themeColorRes) {
        int color = ContextCompat.getColor(activity, themeColorRes);
        activity.getWindow().setStatusBarColor(color);

        // 更新导航栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setNavigationBarColor(color);
        }

        // 更新主题资源
        activity.setTheme(R.style.Theme_PomodoroTimer);
    }
}
