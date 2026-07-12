package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;

/**
 * 设备级 UI 偏好（主题、铃声、应用屏蔽开关等），非用户档案隔离数据。
 */
public class SettingsManager {
    private static final String PREFS_NAME = "PomodoroPrefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_RINGTONE = "ringtone";
    private static final String KEY_APP_BLOCKING_ENABLED = "app_blocking_enabled";
    private static final String KEY_AUTO_DELETE_ENABLED = "auto_delete_enabled";
    private static final String KEY_DARK_MODE_OVERRIDE = "dark_mode_override";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getThemeColor() {
        return prefs.getInt(KEY_THEME, R.color.default_theme);
    }

    public void setThemeColor(int colorRes) {
        prefs.edit()
                .putInt(KEY_THEME, colorRes)
                .remove(KEY_DARK_MODE_OVERRIDE)
                .commit();
    }

    public boolean hasDarkModeOverride() {
        return prefs.contains(KEY_DARK_MODE_OVERRIDE);
    }

    public void setDarkModeOverride() {
        prefs.edit().putBoolean(KEY_DARK_MODE_OVERRIDE, true).apply();
    }

    public String getRingtoneUri() {
        return prefs.getString(KEY_RINGTONE, "default");
    }

    public void setRingtoneUri(String uri) {
        prefs.edit().putString(KEY_RINGTONE, uri).apply();
    }

    public boolean isAppBlockingEnabled() {
        return prefs.getBoolean(KEY_APP_BLOCKING_ENABLED, false);
    }

    public void setAppBlockingEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_APP_BLOCKING_ENABLED, enabled).apply();
    }

    public boolean isAutoDeleteEnabled() {
        return prefs.getBoolean(KEY_AUTO_DELETE_ENABLED, false);
    }

    public void setAutoDeleteEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_DELETE_ENABLED, enabled).apply();
    }
}
