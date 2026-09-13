package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.skyinit.pomodorotimer.domain.timer.SessionPhase;
import com.skyinit.pomodorotimer.domain.timer.TimerSessionPolicy;
import com.skyinit.pomodorotimer.util.CategoryDefaults;

/**
 * 进行中番茄钟会话的磁盘快照，用于进程/服务被杀与重启后恢复。
 * <p>
 * 使用 elapsedRealtime + wall-clock 双时钟；{@code generation} 防止过期 Alarm 误结算。
 * 写入通过单次 {@code commit()} 保证原子性。无 Room 依赖，与数据库版本无关。
 */
public final class ActiveSessionStore {

    private static final String PREFS_NAME = "ActiveSessionPrefs";

    private static final String KEY_ACTIVE = "active";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_GENERATION = "generation";
    private static final String KEY_PHASE = "phase";
    private static final String KEY_TIMER_END_ELAPSED = "timer_end_elapsed";
    private static final String KEY_TIMER_END_WALL = "timer_end_wall";
    private static final String KEY_TIME_LEFT = "time_left";
    private static final String KEY_SAVED_AT_WALL = "saved_at_wall";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_SESSION_TYPE = "session_type";
    private static final String KEY_SESSION_START = "session_start";
    private static final String KEY_PAUSE_COUNT = "pause_count";
    private static final String KEY_PAUSE_START_ELAPSED = "pause_start_elapsed";
    private static final String KEY_PAUSE_START_WALL = "pause_start_wall";
    private static final String KEY_PLANNED_DURATION = "planned_duration";
    private static final String KEY_TASK_ID = "task_id";
    private static final String KEY_SUB_TASK_ID = "sub_task_id";
    private static final String KEY_TASK_TITLE = "task_title";
    private static final String KEY_TASK_CATEGORY = "task_category";
    private static final String KEY_TASK_TAGS = "task_tags";
    private static final String KEY_PAUSE_REASONS = "pause_reasons";
    private static final String KEY_IS_LONG_BREAK = "is_long_break";
    private static final String KEY_AWAITING_POST_BREAK = "awaiting_post_break";
    private static final String KEY_LAST_RESULT = "last_result";
    private static final String KEY_LAST_RESULT_SESSION_ID = "last_result_session_id";

    /** 学习满此时长才建议保存为完成记录（5 分钟）。 */
    public static final long SAVE_ELIGIBLE_MS = 5L * 60L * 1000L;

    /** 最近一次自动结算结果，供 UI 展示结果态。 */
    public static final String RESULT_NONE = "";
    public static final String RESULT_COMPLETED = "completed";
    public static final String RESULT_FAILED_TIMEOUT = "failed_timeout";
    public static final String RESULT_BREAK_STARTED = "break_started";

    private ActiveSessionStore() {
    }

    /** 可恢复的会话快照。 */
    public static final class Checkpoint {
        public final String userId;
        public final long sessionId;
        public final int generation;
        public final SessionPhase phase;
        public final long timerEndElapsedRealtime;
        public final long timerEndWallClockMs;
        public final long timeLeftInMillis;
        public final long savedAtWallClockMs;
        public final boolean running;
        public final boolean paused;
        public final int sessionType;
        public final long sessionStartTime;
        public final int pauseCount;
        public final long pauseStartElapsedRealtime;
        public final long pauseStartWallClockMs;
        public final long plannedDurationMs;
        public final int taskId;
        public final int subTaskId;
        public final String taskTitle;
        public final String category;
        public final String tags;
        public final String pauseReasons;
        public final boolean isLongBreak;
        public final boolean awaitingPostBreakChoice;

        Checkpoint(String userId,
                   long sessionId,
                   int generation,
                   SessionPhase phase,
                   long timerEndElapsedRealtime,
                   long timerEndWallClockMs,
                   long timeLeftInMillis,
                   long savedAtWallClockMs,
                   boolean running,
                   boolean paused,
                   int sessionType,
                   long sessionStartTime,
                   int pauseCount,
                   long pauseStartElapsedRealtime,
                   long pauseStartWallClockMs,
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
            this.sessionId = sessionId;
            this.generation = generation;
            this.phase = phase != null ? phase : SessionPhase.IDLE;
            this.timerEndElapsedRealtime = timerEndElapsedRealtime;
            this.timerEndWallClockMs = timerEndWallClockMs;
            this.timeLeftInMillis = timeLeftInMillis;
            this.savedAtWallClockMs = savedAtWallClockMs;
            this.running = running;
            this.paused = paused;
            this.sessionType = sessionType;
            this.sessionStartTime = sessionStartTime;
            this.pauseCount = pauseCount;
            this.pauseStartElapsedRealtime = pauseStartElapsedRealtime;
            this.pauseStartWallClockMs = pauseStartWallClockMs;
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
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        boolean paused = prefs.getBoolean(KEY_PAUSED, false);
        int sessionType = prefs.getInt(KEY_SESSION_TYPE, 0);
        boolean awaiting = prefs.getBoolean(KEY_AWAITING_POST_BREAK, false);
        String phaseName = prefs.getString(KEY_PHASE, null);
        SessionPhase phase;
        if (phaseName != null) {
            try {
                phase = SessionPhase.valueOf(phaseName);
            } catch (IllegalArgumentException e) {
                phase = SessionPhase.fromFlags(running, paused, sessionType, awaiting);
            }
        } else {
            phase = SessionPhase.fromFlags(running, paused, sessionType, awaiting);
        }
        long sessionStart = prefs.getLong(KEY_SESSION_START, 0L);
        long sessionId = prefs.getLong(KEY_SESSION_ID, sessionStart);
        return new Checkpoint(
                prefs.getString(KEY_USER_ID, ""),
                sessionId,
                prefs.getInt(KEY_GENERATION, 0),
                phase,
                prefs.getLong(KEY_TIMER_END_ELAPSED, 0L),
                prefs.getLong(KEY_TIMER_END_WALL, 0L),
                prefs.getLong(KEY_TIME_LEFT, 0L),
                prefs.getLong(KEY_SAVED_AT_WALL, 0L),
                running,
                paused,
                sessionType,
                sessionStart,
                prefs.getInt(KEY_PAUSE_COUNT, 0),
                prefs.getLong(KEY_PAUSE_START_ELAPSED, 0L),
                prefs.getLong(KEY_PAUSE_START_WALL, 0L),
                prefs.getLong(KEY_PLANNED_DURATION, 0L),
                prefs.getInt(KEY_TASK_ID, -1),
                prefs.getInt(KEY_SUB_TASK_ID, -1),
                prefs.getString(KEY_TASK_TITLE, ""),
                prefs.getString(KEY_TASK_CATEGORY, CategoryDefaults.getDefault()),
                prefs.getString(KEY_TASK_TAGS, ""),
                prefs.getString(KEY_PAUSE_REASONS, ""),
                prefs.getBoolean(KEY_IS_LONG_BREAK, false),
                awaiting
        );
    }

    public static void save(Context context,
                            String userId,
                            long sessionId,
                            int generation,
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
        long nowElapsed = SystemClock.elapsedRealtime();
        long nowWall = System.currentTimeMillis();
        long endWall = 0L;
        if (running && timeLeftInMillis > 0L) {
            endWall = nowWall + timeLeftInMillis;
        } else if (running && timerEndElapsedRealtime > nowElapsed) {
            endWall = nowWall + (timerEndElapsedRealtime - nowElapsed);
        }
        long pauseWall = paused && pauseStartElapsedRealtime > 0L
                ? nowWall - Math.max(0L, nowElapsed - pauseStartElapsedRealtime)
                : (paused ? nowWall : 0L);
        SessionPhase phase = SessionPhase.fromFlags(running, paused, sessionType, awaitingPostBreakChoice);
        long sid = sessionId > 0L ? sessionId : sessionStartTime;

        prefs(context).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putString(KEY_USER_ID, userId != null ? userId : "")
                .putLong(KEY_SESSION_ID, sid)
                .putInt(KEY_GENERATION, generation)
                .putString(KEY_PHASE, phase.name())
                .putLong(KEY_TIMER_END_ELAPSED, timerEndElapsedRealtime)
                .putLong(KEY_TIMER_END_WALL, endWall)
                .putLong(KEY_TIME_LEFT, timeLeftInMillis)
                .putLong(KEY_SAVED_AT_WALL, nowWall)
                .putBoolean(KEY_RUNNING, running)
                .putBoolean(KEY_PAUSED, paused)
                .putInt(KEY_SESSION_TYPE, sessionType)
                .putLong(KEY_SESSION_START, sessionStartTime)
                .putInt(KEY_PAUSE_COUNT, pauseCount)
                .putLong(KEY_PAUSE_START_ELAPSED, pauseStartElapsedRealtime)
                .putLong(KEY_PAUSE_START_WALL, pauseWall)
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
        SharedPreferences prefs = prefs(context);
        String lastResult = prefs.getString(KEY_LAST_RESULT, RESULT_NONE);
        long lastResultSessionId = prefs.getLong(KEY_LAST_RESULT_SESSION_ID, 0L);
        SharedPreferences.Editor editor = prefs.edit().clear();
        if (lastResult != null && !lastResult.isEmpty()) {
            editor.putString(KEY_LAST_RESULT, lastResult)
                    .putLong(KEY_LAST_RESULT_SESSION_ID, lastResultSessionId);
        }
        editor.commit();
    }

    /** 记录自动结算结果供 UI 展示，并清除进行中快照。 */
    public static void markSettledResult(Context context, long sessionId, String result) {
        prefs(context).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putString(KEY_LAST_RESULT, result != null ? result : RESULT_NONE)
                .putLong(KEY_LAST_RESULT_SESSION_ID, sessionId)
                .remove(KEY_RUNNING)
                .remove(KEY_PAUSED)
                .commit();
        // 清掉其余会话字段，保留结果
        SharedPreferences prefs = prefs(context);
        SharedPreferences.Editor editor = prefs.edit().clear();
        editor.putString(KEY_LAST_RESULT, result != null ? result : RESULT_NONE)
                .putLong(KEY_LAST_RESULT_SESSION_ID, sessionId)
                .commit();
    }

    public static String consumeLastResult(Context context) {
        SharedPreferences prefs = prefs(context);
        String result = prefs.getString(KEY_LAST_RESULT, RESULT_NONE);
        if (result == null || result.isEmpty()) {
            return RESULT_NONE;
        }
        prefs.edit()
                .remove(KEY_LAST_RESULT)
                .remove(KEY_LAST_RESULT_SESSION_ID)
                .apply();
        return result;
    }

    public static String peekLastResult(Context context) {
        String result = prefs(context).getString(KEY_LAST_RESULT, RESULT_NONE);
        return result != null ? result : RESULT_NONE;
    }

    public static long peekLastResultSessionId(Context context) {
        return prefs(context).getLong(KEY_LAST_RESULT_SESSION_ID, 0L);
    }

    public static long computeRemainingMillis(Checkpoint cp) {
        if (cp == null) {
            return 0L;
        }
        if (cp.running) {
            long nowElapsed = SystemClock.elapsedRealtime();
            long nowWall = System.currentTimeMillis();
            long remaining = TimerSessionPolicy.resolveRunningRemaining(
                    cp.timerEndElapsedRealtime,
                    cp.timerEndWallClockMs,
                    nowElapsed,
                    nowWall);
            if (cp.timerEndElapsedRealtime <= 0L && cp.timerEndWallClockMs <= 0L
                    && cp.timeLeftInMillis > 0L && cp.savedAtWallClockMs > 0L) {
                return com.skyinit.pomodorotimer.domain.timer.SessionClock.computeRemainingFromSaved(
                        cp.timeLeftInMillis, cp.savedAtWallClockMs, nowWall);
            }
            return remaining;
        }
        return Math.max(0L, cp.timeLeftInMillis);
    }

    public static long computePauseElapsedMillis(Checkpoint cp) {
        if (cp == null || !cp.paused) {
            return 0L;
        }
        return TimerSessionPolicy.resolvePauseElapsed(
                cp.pauseStartElapsedRealtime,
                cp.pauseStartWallClockMs,
                SystemClock.elapsedRealtime(),
                System.currentTimeMillis());
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
