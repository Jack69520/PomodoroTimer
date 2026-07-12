package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.skyinit.pomodorotimer.util.CategoryDefaults;

/**
 * 进行中番茄钟会话的磁盘快照，用于进程/服务被杀后恢复。
 */
public final class ActiveSessionStore {

    private static final String PREFS_NAME = "ActiveSessionPrefs";

    private static final String KEY_ACTIVE = "active";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_TIMER_END_ELAPSED = "timer_end_elapsed";
    private static final String KEY_TIME_LEFT = "time_left";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_SESSION_TYPE = "session_type";
    private static final String KEY_SESSION_START = "session_start";
    private static final String KEY_PAUSE_COUNT = "pause_count";
    private static final String KEY_PAUSE_START_ELAPSED = "pause_start_elapsed";
    private static final String KEY_PLANNED_DURATION = "planned_duration";
    private static final String KEY_TASK_ID = "task_id";
    private static final String KEY_SUB_TASK_ID = "sub_task_id";
    private static final String KEY_TASK_TITLE = "task_title";
    private static final String KEY_TASK_CATEGORY = "task_category";
    private static final String KEY_TASK_TAGS = "task_tags";
    private static final String KEY_PAUSE_REASONS = "pause_reasons";
    private static final String KEY_IS_LONG_BREAK = "is_long_break";
    private static final String KEY_AWAITING_POST_BREAK = "awaiting_post_break";

    /** 学习满此时长才建议保存为完成记录（5 分钟）。 */
    public static final long SAVE_ELIGIBLE_MS = 5L * 60L * 1000L;

    private ActiveSessionStore() {
    }

    /** 可恢复的会话快照。 */
    public static final class Checkpoint {
        public final String userId;
        public final long timerEndElapsedRealtime;
        public final long timeLeftInMillis;
        public final boolean running;
        public final boolean paused;
        public final int sessionType;
        public final long sessionStartTime;
        public final int pauseCount;
        public final long pauseStartElapsedRealtime;
        public final long plannedDurationMs;
        public final int taskId;
        public final int subTaskId;
        public final String taskTitle;
        public final String category;
        public final String tags;
        public final String pauseReasons;
        public final boolean isLongBreak;
        public final boolean awaitingPostBreakChoice;

        private Checkpoint(String userId,
                           long timerEndElapsedRealtime,
                           long timeLeftInMillis,
                           boolean running,
                           boolean paused,
                           int sessionType,
                           long sessionStartTime,
                           int pauseCount,
                           long pauseStartElapsedRealtime,
                           long plannedDurationMs,
                           int taskId,
                           int subTaskId,
                           String taskTitle,
                           String category,
                           String tags,
                           String pauseReasons,
                           boolean isLongBreak,
                           boolean awaitingPostBreakChoice) {
            this.userId = userId != null ? userId : "";
            this.timerEndElapsedRealtime = timerEndElapsedRealtime;
            this.timeLeftInMillis = timeLeftInMillis;
            this.running = running;
            this.paused = paused;
            this.sessionType = sessionType;
            this.sessionStartTime = sessionStartTime;
            this.pauseCount = pauseCount;
            this.pauseStartElapsedRealtime = pauseStartElapsedRealtime;
            this.plannedDurationMs = plannedDurationMs;
            this.taskId = taskId;
            this.subTaskId = subTaskId;
            this.taskTitle = taskTitle != null ? taskTitle : "";
            this.category = category != null ? category : CategoryDefaults.getDefault();
            this.tags = tags != null ? tags : "";
            this.pauseReasons = pauseReasons != null ? pauseReasons : "";
            this.isLongBreak = isLongBreak;
            this.awaitingPostBreakChoice = awaitingPostBreakChoice;
        }

        public boolean belongsToUser(String activeUserId) {
            if (activeUserId == null || activeUserId.isEmpty()) {
                return false;
            }
            return activeUserId.equals(userId);
        }
    }

    public static boolean hasActiveSession(Context context) {
        return prefs(context).getBoolean(KEY_ACTIVE, false);
    }

    public static Checkpoint load(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.getBoolean(KEY_ACTIVE, false)) {
            return null;
        }
        return new Checkpoint(
                prefs.getString(KEY_USER_ID, ""),
                prefs.getLong(KEY_TIMER_END_ELAPSED, 0L),
                prefs.getLong(KEY_TIME_LEFT, 0L),
                prefs.getBoolean(KEY_RUNNING, false),
                prefs.getBoolean(KEY_PAUSED, false),
                prefs.getInt(KEY_SESSION_TYPE, 0),
                prefs.getLong(KEY_SESSION_START, 0L),
                prefs.getInt(KEY_PAUSE_COUNT, 0),
                prefs.getLong(KEY_PAUSE_START_ELAPSED, 0L),
                prefs.getLong(KEY_PLANNED_DURATION, 0L),
                prefs.getInt(KEY_TASK_ID, -1),
                prefs.getInt(KEY_SUB_TASK_ID, -1),
                prefs.getString(KEY_TASK_TITLE, ""),
                prefs.getString(KEY_TASK_CATEGORY, CategoryDefaults.getDefault()),
                prefs.getString(KEY_TASK_TAGS, ""),
                prefs.getString(KEY_PAUSE_REASONS, ""),
                prefs.getBoolean(KEY_IS_LONG_BREAK, false),
                prefs.getBoolean(KEY_AWAITING_POST_BREAK, false)
        );
    }

    public static void save(Context context,
                            String userId,
                            long timerEndElapsedRealtime,
                            long timeLeftInMillis,
                            boolean running,
                            boolean paused,
                            int sessionType,
                            long sessionStartTime,
                            int pauseCount,
                            long pauseStartElapsedRealtime,
                            long plannedDurationMs,
                            int taskId,
                            int subTaskId,
                            String taskTitle,
                            String category,
                            String tags,
                            String pauseReasons,
                            boolean isLongBreak,
                            boolean awaitingPostBreakChoice) {
        prefs(context).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putString(KEY_USER_ID, userId != null ? userId : "")
                .putLong(KEY_TIMER_END_ELAPSED, timerEndElapsedRealtime)
                .putLong(KEY_TIME_LEFT, timeLeftInMillis)
                .putBoolean(KEY_RUNNING, running)
                .putBoolean(KEY_PAUSED, paused)
                .putInt(KEY_SESSION_TYPE, sessionType)
                .putLong(KEY_SESSION_START, sessionStartTime)
                .putInt(KEY_PAUSE_COUNT, pauseCount)
                .putLong(KEY_PAUSE_START_ELAPSED, pauseStartElapsedRealtime)
                .putLong(KEY_PLANNED_DURATION, plannedDurationMs)
                .putInt(KEY_TASK_ID, taskId)
                .putInt(KEY_SUB_TASK_ID, subTaskId)
                .putString(KEY_TASK_TITLE, taskTitle != null ? taskTitle : "")
                .putString(KEY_TASK_CATEGORY, category != null ? category : CategoryDefaults.getDefault())
                .putString(KEY_TASK_TAGS, tags != null ? tags : "")
                .putString(KEY_PAUSE_REASONS, pauseReasons != null ? pauseReasons : "")
                .putBoolean(KEY_IS_LONG_BREAK, isLongBreak)
                .putBoolean(KEY_AWAITING_POST_BREAK, awaitingPostBreakChoice)
                .commit();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().commit();
    }

    public static long computeRemainingMillis(Checkpoint cp) {
        if (cp == null) {
            return 0L;
        }
        if (cp.running) {
            return Math.max(0L, cp.timerEndElapsedRealtime - SystemClock.elapsedRealtime());
        }
        return Math.max(0L, cp.timeLeftInMillis);
    }

    public static long computeElapsedMillis(Checkpoint cp) {
        if (cp == null) {
            return 0L;
        }
        long planned = cp.plannedDurationMs;
        if (planned <= 0L) {
            planned = cp.timeLeftInMillis + Math.max(0L, System.currentTimeMillis() - cp.sessionStartTime);
        }
        long remaining = computeRemainingMillis(cp);
        return Math.max(0L, planned - remaining);
    }

    public static boolean isSaveEligible(Checkpoint cp) {
        return cp != null && cp.sessionType == 0 && computeElapsedMillis(cp) >= SAVE_ELIGIBLE_MS;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
