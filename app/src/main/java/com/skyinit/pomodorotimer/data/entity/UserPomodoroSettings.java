package com.skyinit.pomodorotimer.data.entity;



import androidx.annotation.NonNull;

import androidx.room.Entity;

import androidx.room.Ignore;

import androidx.room.PrimaryKey;



/**

 * 用户级番茄钟工作流与计时偏好，按 userId 持久化。

 */

@Entity(tableName = "user_pomodoro_settings")

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



    @PrimaryKey

    @NonNull

    public String userId;



    /** 默认学习时长（毫秒）。 */

    public long defaultStudyTimeMs;



    /** 默认休息时长（毫秒）。 */

    public long defaultBreakTimeMs;



    /** 单次番茄允许的最大暂停次数。 */

    public int maxPauseCount;



    /** 学习计时期间是否启用勿扰（DND）模式。 */

    public boolean dndDuringFocusEnabled;



    /** 休息结束后是否自动开始下一轮番茄计时，默认关闭。 */

    public boolean autoStartAfterBreak;



    /** 是否启用每 N 个番茄钟后的长休息。 */

    public boolean longBreakEnabled;



    /** 长休息触发间隔（N ≥ 2）。 */

    public int pomodorosBeforeLongBreak;



    /** 长休息时长（10~15 分钟）。 */

    public long longBreakDurationMs;



    /** 当前周期内已完成的番茄钟数，用于长休息判定。 */

    public int pomodoroCycleCount;



    public UserPomodoroSettings() {

        this.userId = "";

        applyDefaults();

    }



    @Ignore

    public UserPomodoroSettings(@NonNull String userId) {

        this.userId = userId;

        applyDefaults();

    }



    private void applyDefaults() {

        defaultStudyTimeMs = DEFAULT_STUDY_TIME_MS;

        defaultBreakTimeMs = DEFAULT_BREAK_TIME_MS;

        maxPauseCount = DEFAULT_MAX_PAUSE_COUNT;

        dndDuringFocusEnabled = false;

        autoStartAfterBreak = false;

        longBreakEnabled = false;

        pomodorosBeforeLongBreak = DEFAULT_POMODOROS_BEFORE_LONG_BREAK;

        longBreakDurationMs = DEFAULT_LONG_BREAK_MS;

        pomodoroCycleCount = 0;

    }

}


