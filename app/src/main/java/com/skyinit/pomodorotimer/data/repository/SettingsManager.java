package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.ui.theme.WallpaperCatalog;

/**
 * Device-level preferences: wallpaper, ringtone, auto-delete, and pomodoro workflow settings.
 */
public class SettingsManager {
    private static final String PREFS_NAME = "PomodoroPrefs";
    private static final String KEY_WALLPAPER_THEME = "wallpaper_theme_key";
    private static final String KEY_RINGTONE = "ringtone";
    private static final String KEY_AUTO_DELETE_ENABLED = "auto_delete_enabled";
    /** 待办集父卡是否显示进度条与「下一步」；默认关以保持紧凑。 */
    private static final String KEY_COLLECTION_PROGRESS_DETAILS = "todo_collection_progress_details";
    private static final String KEY_DARK_MODE_OVERRIDE = "dark_mode_override";

    private static final String KEY_DEFAULT_STUDY_MS = "pomodoro_default_study_ms";
    private static final String KEY_DEFAULT_BREAK_MS = "pomodoro_default_break_ms";
    private static final String KEY_MAX_PAUSE_COUNT = "pomodoro_max_pause_count";
    private static final String KEY_DND_DURING_FOCUS = "pomodoro_dnd_during_focus";
    private static final String KEY_AUTO_BLOCK_DURING = "pomodoro_auto_block_during";
    private static final String KEY_LOCK_SCREEN_FULLSCREEN = "pomodoro_lock_screen_fullscreen";
    private static final String KEY_AUTO_START_AFTER_BREAK = "pomodoro_auto_start_after_break";
    private static final String KEY_LONG_BREAK_ENABLED = "pomodoro_long_break_enabled";
    private static final String KEY_POMODOROS_BEFORE_LONG = "pomodoro_pomodoros_before_long";
    private static final String KEY_LONG_BREAK_DURATION_MS = "pomodoro_long_break_duration_ms";
    private static final String KEY_POMODORO_CYCLE_COUNT = "pomodoro_cycle_count";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public String getWallpaperThemeKey() {
        String key = prefs.getString(KEY_WALLPAPER_THEME, WallpaperCatalog.DEFAULT_KEY);
        if (key == null || key.trim().isEmpty()) {
            return WallpaperCatalog.DEFAULT_KEY;
        }
        return key;
    }

    public void setWallpaperThemeKey(@Nullable String key) {
        String normalized = (key == null || key.trim().isEmpty())
                ? WallpaperCatalog.DEFAULT_KEY
                : key.trim();
        prefs.edit()
                .putString(KEY_WALLPAPER_THEME, normalized)
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

    public boolean isAutoDeleteEnabled() {
        return prefs.getBoolean(KEY_AUTO_DELETE_ENABLED, false);
    }

    public void setAutoDeleteEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_DELETE_ENABLED, enabled).apply();
    }

    /** 待办集列表父卡：进度条 + 下一步文案。默认 false。 */
    public boolean isCollectionProgressDetailsEnabled() {
        return prefs.getBoolean(KEY_COLLECTION_PROGRESS_DETAILS, false);
    }

    public void setCollectionProgressDetailsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_COLLECTION_PROGRESS_DETAILS, enabled).apply();
    }

    @NonNull
    public UserPomodoroSettings getPomodoroSettings() {
        UserPomodoroSettings s = new UserPomodoroSettings();
        s.defaultStudyTimeMs = prefs.getLong(KEY_DEFAULT_STUDY_MS, UserPomodoroSettings.DEFAULT_STUDY_TIME_MS);
        s.defaultBreakTimeMs = prefs.getLong(KEY_DEFAULT_BREAK_MS, UserPomodoroSettings.DEFAULT_BREAK_TIME_MS);
        s.maxPauseCount = prefs.getInt(KEY_MAX_PAUSE_COUNT, UserPomodoroSettings.DEFAULT_MAX_PAUSE_COUNT);
        s.dndDuringFocusEnabled = prefs.getBoolean(KEY_DND_DURING_FOCUS, false);
        s.autoBlockDuringPomodoro = prefs.getBoolean(KEY_AUTO_BLOCK_DURING, false);
        s.lockScreenFullscreenEnabled = prefs.getBoolean(KEY_LOCK_SCREEN_FULLSCREEN, false);
        s.autoStartAfterBreak = prefs.getBoolean(KEY_AUTO_START_AFTER_BREAK, false);
        s.longBreakEnabled = prefs.getBoolean(KEY_LONG_BREAK_ENABLED, false);
        s.pomodorosBeforeLongBreak = prefs.getInt(
                KEY_POMODOROS_BEFORE_LONG, UserPomodoroSettings.DEFAULT_POMODOROS_BEFORE_LONG_BREAK);
        s.longBreakDurationMs = prefs.getLong(
                KEY_LONG_BREAK_DURATION_MS, UserPomodoroSettings.DEFAULT_LONG_BREAK_MS);
        s.pomodoroCycleCount = prefs.getInt(KEY_POMODORO_CYCLE_COUNT, 0);
        return s;
    }

    public void savePomodoroSettings(@NonNull UserPomodoroSettings settings) {
        prefs.edit()
                .putLong(KEY_DEFAULT_STUDY_MS, settings.defaultStudyTimeMs)
                .putLong(KEY_DEFAULT_BREAK_MS, settings.defaultBreakTimeMs)
                .putInt(KEY_MAX_PAUSE_COUNT, settings.maxPauseCount)
                .putBoolean(KEY_DND_DURING_FOCUS, settings.dndDuringFocusEnabled)
                .putBoolean(KEY_AUTO_BLOCK_DURING, settings.autoBlockDuringPomodoro)
                .putBoolean(KEY_LOCK_SCREEN_FULLSCREEN, settings.lockScreenFullscreenEnabled)
                .putBoolean(KEY_AUTO_START_AFTER_BREAK, settings.autoStartAfterBreak)
                .putBoolean(KEY_LONG_BREAK_ENABLED, settings.longBreakEnabled)
                .putInt(KEY_POMODOROS_BEFORE_LONG, settings.pomodorosBeforeLongBreak)
                .putLong(KEY_LONG_BREAK_DURATION_MS, settings.longBreakDurationMs)
                .putInt(KEY_POMODORO_CYCLE_COUNT, settings.pomodoroCycleCount)
                .commit();
    }

    public void clearPomodoroCycleCount() {
        prefs.edit().putInt(KEY_POMODORO_CYCLE_COUNT, 0).commit();
    }
}
