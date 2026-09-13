package com.skyinit.pomodorotimer.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 设备级番茄钟工作流与计时偏好快照（非 Room 实体；持久化于 SharedPreferences）。
 * {@link #pomodoroCycleCount} 为运行时周期计数，切号/登出时清零。
 */
public class UserPomodoroSettings {

    public static final int MIN_POMODOROS_BEFORE_LONG_BREAK = 2;
    public static final int DEFAULT_POMODOROS_BEFORE_LONG_BREAK = 4;
    public static final long MIN_LONG_BREAK_MS = 10L * 60L * 1000L;
    public static final long MAX_LONG_BREAK_MS = 15L * 60L * 1000L;
    public static final long DEFAULT_LONG_BREAK_MS = 15L * 60L * 1000L;

    public static final long DEFAULT_STUDY_TIME_MS = 25L * 60L * 1000L;
    public static final long DEFAULT_BREAK_TIME_MS = 5L * 60L * 1000L;
    public static final int DEFAULT_MAX_PAUSE_COUNT = 2;
    public static final int MIN_MAX_PAUSE_COUNT = 1;
    public static final int MAX_MAX_PAUSE_COUNT = 5;

    /** 兼容旧调用；设备级设置下可为空。 */
    @NonNull
    public String userId = "";

    public long defaultStudyTimeMs;
    public long defaultBreakTimeMs;
    public int maxPauseCount;
    public boolean dndDuringFocusEnabled;
    public boolean autoBlockDuringPomodoro;
    /** 专注/休息期间锁屏全屏显示计时页（默认关）。 */
    public boolean lockScreenFullscreenEnabled;
    public boolean autoStartAfterBreak;
    public boolean longBreakEnabled;
    public int pomodorosBeforeLongBreak;
    public long longBreakDurationMs;
    public int pomodoroCycleCount;

    public UserPomodoroSettings() {
        applyDefaults();
    }

    public UserPomodoroSettings(@Nullable String userId) {
        this.userId = userId != null ? userId : "";
        applyDefaults();
    }

    private void applyDefaults() {
        defaultStudyTimeMs = DEFAULT_STUDY_TIME_MS;
        defaultBreakTimeMs = DEFAULT_BREAK_TIME_MS;
        maxPauseCount = DEFAULT_MAX_PAUSE_COUNT;
        dndDuringFocusEnabled = false;
        autoBlockDuringPomodoro = false;
        lockScreenFullscreenEnabled = false;
        autoStartAfterBreak = false;
        longBreakEnabled = false;
        pomodorosBeforeLongBreak = DEFAULT_POMODOROS_BEFORE_LONG_BREAK;
        longBreakDurationMs = DEFAULT_LONG_BREAK_MS;
        pomodoroCycleCount = 0;
    }
}
