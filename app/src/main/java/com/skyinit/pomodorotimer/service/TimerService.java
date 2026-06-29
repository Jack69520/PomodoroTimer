package com.skyinit.pomodorotimer.service;

import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerStateRepository;
import com.skyinit.pomodorotimer.data.repository.UserPomodoroSettingsRepository;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.util.NotificationPermission;
import com.skyinit.pomodorotimer.util.VibrationHelper;
import com.skyinit.pomodorotimer.ui.home.TimerActivity;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.R;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.CategoryDefaults;
import com.skyinit.pomodorotimer.util.FocusDndHelper;
import com.skyinit.pomodorotimer.util.SessionPauseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TimerService extends Service {
    private static final String TAG = "TimerService";
    private static final String CHANNEL_ID = "TimerChannelOngoing";
    private static final String CHANNEL_ID_ALERTS = "TimerAlertChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long TICK_INTERVAL_MS = 1000L;
    /** 暂停超过此时长判定失败。 */
    private static final long PAUSE_TIMEOUT = 5 * 60 * 1000L;
    /** 运行中 checkpoint 落盘节流间隔，避免每秒写盘。 */
    private static final long CHECKPOINT_SAVE_INTERVAL_MS = 10_000L;

    public static final String ACTION_START = "START";
    public static final String ACTION_END_BREAK = "END_BREAK";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_RESUME = "RESUME";
    public static final String ACTION_RESET = "RESET";
    public static final String ACTION_PAUSE_WITH_REASON = "PAUSE_WITH_REASON";
    public static final String ACTION_PAUSE_TIMEOUT = "PAUSE_TIMEOUT";
    /** Alarm 触发的会话到点完成（进程被杀后的兜底入口）。 */
    public static final String ACTION_SESSION_COMPLETE = "SESSION_COMPLETE";
    /** 用户确认后从快照恢复计时。 */
    public static final String ACTION_RESTORE_SESSION = "RESTORE_SESSION";
    /** 用户主动结束中断的会话。 */
    public static final String ACTION_END_INTERRUPTED = "END_INTERRUPTED";
    public static final String EXTRA_SAVE_RECORD = "save_record";
    public static final String ACTION_ACTIVITY_ENDED_BROADCAST = "com.skyinit.pomodorotimer.ACTION_ACTIVITY_ENDED";
    public static final String ACTION_FORCE_FAIL = "FORCE_FAIL";

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_CATEGORY = "task_category";
    public static final String EXTRA_TASK_TAGS = "task_tags";
    public static final String EXTRA_SUB_TASK_ID = "sub_task_id";
    public static final String EXTRA_SUB_TASK_TITLE = "sub_task_title";
    public static final String EXTRA_POST_STUDY = "post_study";
    public static final String EXTRA_POST_BREAK = "post_break";
    /** 覆盖默认学习时长（毫秒），用于快捷方式等场景。 */
    public static final String EXTRA_STUDY_DURATION_MS = "study_duration_ms";

    private final IBinder binder = new LocalBinder();

    private Handler timerHandler;
    private Runnable tickRunnable;
    private Handler pauseTimeoutHandler;
    private Runnable pauseTimeoutRunnable;

    private long timeLeftInMillis;
    private long timerEndElapsedRealtime;
    private boolean isRunning;
    private boolean isPaused;
    private final CopyOnWriteArrayList<TimerListener> listeners = new CopyOnWriteArrayList<>();
    private int sessionType; // 0 = 学习, 1 = 休息
    private boolean isLongBreak;
    private boolean awaitingPostBreakChoice;
    private StatisticsRepository statisticsRepository;
    private TimerStateRepository timerStateRepository;
    private TimerSettingsRepository timerSettingsRepository;
    private UserPomodoroSettingsRepository pomodoroSettingsRepository;
    private SettingsManager settingsManager;
    private long sessionStartTime;
    private int currentTaskId = -1;
    private int currentSubTaskId = -1;
    private String currentTaskTitle = "";
    private String currentCategory = CategoryDefaults.getDefault();
    private String currentTags = "";

    private int pauseCount;
    private final List<String> sessionPauseReasons = new ArrayList<>();
    private long pauseStartElapsedRealtime;
    private long plannedDurationMs;
    private long lastCheckpointSaveElapsed;
    /** 标记是否已从磁盘恢复，避免 onCreate 与 onStartCommand 重复恢复。 */
    private boolean restoredFromCheckpoint;
    /** 防止 Alarm 恢复与 handleSessionCompleteAlarm 重复触发完成逻辑。 */
    private boolean isCompletingSession;

    public class LocalBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }

    public interface TimerListener {
        void onTimerTick(long millisUntilFinished);
        void onTimerFinish();
        void onTimerReset();
        void onTimerStateChanged();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        AppContainer container = AppContainer.getInstance(this);
        timerSettingsRepository = container.getTimerSettingsRepository();
        statisticsRepository = container.getStatisticsRepository();
        timerStateRepository = container.getTimerStateRepository();
        pomodoroSettingsRepository = container.getUserPomodoroSettingsRepository();
        pomodoroSettingsRepository.warmCache();
        timeLeftInMillis = timerSettingsRepository.getDefaultStudyTimeMs();
        settingsManager = container.getSettingsManager();
        timerHandler = new Handler(Looper.getMainLooper());
        pauseTimeoutHandler = new Handler(Looper.getMainLooper());
        tickRunnable = this::onTick;

        // 存在快照时暂不自动恢复，等待用户在 MainActivity 弹窗中选择；同时取消旧 Alarm 避免对话框期间误触发
        if (ActiveSessionStore.hasActiveSession(this)) {
            TimerAlarmScheduler.cancelAll(this);
        }

        publishState();
    }

    private long getDefaultStudyTimeMs() {
        return timerSettingsRepository.getDefaultStudyTimeMs();
    }

    private long getBreakTimeMs() {
        return timerSettingsRepository.getDefaultBreakTimeMs();
    }

    private void publishState() {
        timerStateRepository.publish(
                getTimeLeft(), isRunning, isPaused, sessionType, awaitingPostBreakChoice, isLongBreak);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // startForegroundService() 要求在约 5 秒内调用 startForeground()，否则系统 ANR
        promoteToForegroundImmediately();

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                awaitingPostBreakChoice = false;
                long customStudyMs = intent.getLongExtra(EXTRA_STUDY_DURATION_MS, -1L);
                if (sessionType == 1 && !isRunning && !isPaused && timeLeftInMillis > 0L) {
                    isLongBreak = false;
                    timeLeftInMillis = getBreakTimeMs();
                    beginNewSession();
                } else {
                    sessionType = 0;
                    isLongBreak = false;
                    if (customStudyMs > 0L) {
                        timeLeftInMillis = TimerSettingsRepository.clampStudy(customStudyMs);
                    } else {
                        timeLeftInMillis = getDefaultStudyTimeMs();
                    }
                    beginNewSession();
                }
            } else if (ACTION_END_BREAK.equals(action)) {
                endBreakEarly();
            } else if (ACTION_PAUSE.equals(action)) {
                pauseTimer();
            } else if (ACTION_RESUME.equals(action)) {
                resumeTimer();
            } else if (ACTION_RESET.equals(action)) {
                resetTimer();
            } else if (ACTION_PAUSE_WITH_REASON.equals(action)) {
                pauseTimerWithReason(intent.getStringExtra("pause_reason"));
            } else if (ACTION_FORCE_FAIL.equals(action)) {
                endSessionDueToLeave();
            } else if (ACTION_PAUSE_TIMEOUT.equals(action)) {
                handlePauseTimeoutAlarm();
            } else if (ACTION_SESSION_COMPLETE.equals(action)) {
                handleSessionCompleteAlarm();
            } else if (ACTION_RESTORE_SESSION.equals(action)) {
                restoreFromCheckpoint();
            } else if (ACTION_END_INTERRUPTED.equals(action)) {
                boolean saveRecord = intent.getBooleanExtra(EXTRA_SAVE_RECORD, false);
                endInterruptedSession(saveRecord);
            }
        }

        // 空闲时降为后台服务，避免常驻无意义的前台通知
        downgradeForegroundIfIdle();
        return START_STICKY;
    }

    /**
     * 立即进入前台状态，满足 startForegroundService 的时限要求。
     * 空闲/等待用户恢复时使用轻量通知，运行/暂停时使用计时通知。
     */
    private void promoteToForegroundImmediately() {
        Notification notification;
        if (isRunning || isPaused) {
            notification = createNotification();
        } else if (ActiveSessionStore.hasActiveSession(this)) {
            notification = createPendingRecoveryNotification();
        } else {
            notification = createIdleNotification();
        }
        enterForeground(notification);
    }

    /** 非运行态时降回后台，startForeground 要求已在前序 promote 中满足。 */
    private void downgradeForegroundIfIdle() {
        if (!isRunning && !isPaused) {
            stopForeground(STOP_FOREGROUND_DETACH);
        }
    }

    /**
     * 从 ActiveSessionStore 恢复会话。若计时已过期则立即完成；若暂停已超时则判定失败。
     * 供用户确认继续或 Alarm 兜底时调用。
     */
    private void restoreFromCheckpoint() {
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(this);
        if (cp == null) {
            return;
        }

        restoredFromCheckpoint = true;
        sessionType = cp.sessionType;
        isLongBreak = cp.isLongBreak;
        awaitingPostBreakChoice = cp.awaitingPostBreakChoice;
        sessionStartTime = cp.sessionStartTime;
        pauseCount = cp.pauseCount;
        sessionPauseReasons.clear();
        sessionPauseReasons.addAll(SessionPauseUtils.decodeReasons(cp.pauseReasons, null));
        currentTaskId = cp.taskId;
        currentSubTaskId = cp.subTaskId;
        currentTaskTitle = cp.taskTitle;
        currentCategory = cp.category;
        currentTags = cp.tags;
        plannedDurationMs = cp.plannedDurationMs;

        AppLog.d(TAG, "Restoring active session: running=" + cp.running + ", paused=" + cp.paused);

        if (cp.running) {
            timerEndElapsedRealtime = cp.timerEndElapsedRealtime;
            long remaining = timerEndElapsedRealtime - SystemClock.elapsedRealtime();
            if (remaining <= 0L) {
                // 必须先置为 false，否则 handleSessionCompleteAlarm 会再次调用 onTimerComplete
                isRunning = false;
                isPaused = false;
                timeLeftInMillis = 0L;
                onTimerComplete();
                return;
            }
            isRunning = true;
            isPaused = false;
            timeLeftInMillis = remaining;
            startForegroundWithNotification();
            startTimerLoop();
            TimerAlarmScheduler.scheduleSessionComplete(this, timerEndElapsedRealtime);
            if (sessionType == 0 && settingsManager.isAppBlockingEnabled()) {
                startAppBlockingService();
            }
            notifyStateChanged();
        } else if (cp.paused) {
            isPaused = true;
            isRunning = false;
            timeLeftInMillis = cp.timeLeftInMillis;
            pauseStartElapsedRealtime = cp.pauseStartElapsedRealtime;
            long pauseElapsed = SystemClock.elapsedRealtime() - pauseStartElapsedRealtime;
            if (pauseElapsed >= PAUSE_TIMEOUT) {
                timerHandler.post(this::endSessionDueToTimeout);
                return;
            }
            startPauseTimeoutCheck();
            updateNotification();
            notifyStateChanged();
        } else {
            clearCheckpoint();
        }
    }

    /** 用户选择结束中断会话：按是否保存写入数据库或仅清除快照。 */
    private void endInterruptedSession(boolean saveRecord) {
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(this);
        if (cp == null) {
            resetTimerQuietly();
            return;
        }

        long elapsed = ActiveSessionStore.computeElapsedMillis(cp);
        if (saveRecord && cp.sessionType == 0 && elapsed >= ActiveSessionStore.SAVE_ELIGIBLE_MS) {
            String userId = AccountManager.getInstance(this).requireActiveUserId();
            statisticsRepository.recordSession(
                    userId,
                    cp.sessionStartTime,
                    elapsed,
                    cp.taskId,
                    cp.subTaskId,
                    cp.category,
                    cp.tags,
                    cp.pauseCount,
                    SessionPauseUtils.decodeReasons(cp.pauseReasons, null),
                    true,
                    null
            );
        }

        if (cp.sessionType == 0) {
            stopAppBlockingService();
            FocusDndHelper.restoreDnd(this);
        }

        clearCheckpoint();
        resetTimerQuietly();
    }

    /** 结束中断会话后重置为空闲，不触发 stopSelf（保持 Service 绑定）。 */
    private void resetTimerQuietly() {
        cancelPauseTimeoutCheck();
        stopTimerLoop();
        isRunning = false;
        isPaused = false;
        pauseCount = 0;
        sessionPauseReasons.clear();
        pauseStartElapsedRealtime = 0L;
        plannedDurationMs = 0L;
        timeLeftInMillis = getDefaultStudyTimeMs();
        sessionType = 0;
        isLongBreak = false;
        awaitingPostBreakChoice = false;
        clearCurrentTask();
        publishState();
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    /** 全新开始一轮计时（重置暂停次数与会话起点）。 */
    private void beginNewSession() {
        if (isRunning) {
            return;
        }

        cancelPauseTimeoutCheck();
        isRunning = true;
        isPaused = false;
        pauseCount = 0;
        sessionPauseReasons.clear();
        pauseStartElapsedRealtime = 0L;
        sessionStartTime = System.currentTimeMillis();
        plannedDurationMs = timeLeftInMillis;

        if (sessionType == 0 && settingsManager.isAppBlockingEnabled()) {
            startAppBlockingService();
        }
        if (sessionType == 0) {
            FocusDndHelper.maybeEnableDnd(this, timerSettingsRepository.isDndDuringFocusEnabled());
        }

        startForegroundWithNotification();
        startTimerLoop();
        notifyStateChanged();
    }

    /** 从暂停恢复，保留 pauseCount 与 sessionStartTime。 */
    private void resumeTimer() {
        if (!isPaused || isRunning) {
            return;
        }

        cancelPauseTimeoutCheck();
        isPaused = false;
        pauseStartElapsedRealtime = 0L;
        isRunning = true;

        startForegroundWithNotification();
        startTimerLoop();
        notifyStateChanged();
    }

    private void startTimerLoop() {
        timerEndElapsedRealtime = SystemClock.elapsedRealtime() + timeLeftInMillis;
        timerHandler.removeCallbacks(tickRunnable);
        timerHandler.post(tickRunnable);
        // Handler tick 之外，Alarm 负责到点兜底
        TimerAlarmScheduler.scheduleSessionComplete(this, timerEndElapsedRealtime);
    }

    private void stopTimerLoop() {
        timerHandler.removeCallbacks(tickRunnable);
        TimerAlarmScheduler.cancelSessionComplete(this);
    }

    private void onTick() {
        if (!isRunning) {
            return;
        }

        long remaining = timerEndElapsedRealtime - SystemClock.elapsedRealtime();
        if (remaining <= 0L) {
            timeLeftInMillis = 0L;
            onTimerComplete();
            return;
        }

        timeLeftInMillis = remaining;
        notifyTimerTick(remaining);
        updateNotification();
        maybeSaveCheckpointThrottled();
        timerHandler.postDelayed(tickRunnable, TICK_INTERVAL_MS);
    }

    private void onTimerComplete() {
        if (isCompletingSession) {
            AppLog.d(TAG, "Ignoring duplicate onTimerComplete");
            return;
        }
        isCompletingSession = true;
        try {
            stopTimerLoop();
            isRunning = false;
            isPaused = false;
            cancelPauseTimeoutCheck();
            clearCheckpoint();

            int completedSessionType = sessionType;

            notifyTimerFinish();
            stopAppBlockingService();
            FocusDndHelper.restoreDnd(this);

            if (completedSessionType == 0) {
                long duration = System.currentTimeMillis() - sessionStartTime;
                String userId = AccountManager.getInstance(this).requireActiveUserId();
                statisticsRepository.recordSession(
                        userId, sessionStartTime, duration, currentTaskId, currentSubTaskId,
                        currentCategory, currentTags,
                        pauseCount, new ArrayList<>(sessionPauseReasons), false, null);
                if (currentTaskId >= 0) {
                    incrementTaskCompletedPomodoros(currentTaskId);
                }

                prepareAndAutoStartBreak();
            } else {
                handleBreakComplete();
            }
            if (!isRunning && !isPaused) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            }
        } finally {
            isCompletingSession = false;
        }
    }

    /** 学习完成后自动进入休息（短休息或长休息）。 */
    private void prepareAndAutoStartBreak() {
        UserPomodoroSettings settings = pomodoroSettingsRepository.getSettings();
        int newCount = settings.pomodoroCycleCount + 1;
        boolean useLongBreak = settings.longBreakEnabled
                && newCount >= settings.pomodorosBeforeLongBreak;

        sessionType = 1;
        awaitingPostBreakChoice = false;
        if (useLongBreak) {
            settings.pomodoroCycleCount = 0;
            isLongBreak = true;
            timeLeftInMillis = UserPomodoroSettingsRepository.clampLongBreakDurationMs(
                    settings.longBreakDurationMs);
        } else {
            settings.pomodoroCycleCount = newCount;
            isLongBreak = false;
            timeLeftInMillis = getBreakTimeMs();
        }
        pomodoroSettingsRepository.saveSettings(settings);

        beginNewSession();
        sendBreakStartedNotification();
        openTimerForBreakSession();
    }

    /** 休息自然结束：按设置自动开始下一轮或等待用户选择。 */
    private void handleBreakComplete() {
        isLongBreak = false;

        UserPomodoroSettings settings = pomodoroSettingsRepository.getSettings();
        if (settings.autoStartAfterBreak) {
            awaitingPostBreakChoice = false;
            sessionType = 0;
            timeLeftInMillis = getDefaultStudyTimeMs();
            publishState();
            beginNewSession();
            sendBreakEndedNotification();
            sendNextPomodoroStartedNotification();
        } else {
            sendBreakEndedNotification();
            sessionType = 0;
            timeLeftInMillis = getDefaultStudyTimeMs();
            awaitingPostBreakChoice = true;
            publishState();
            openTimerForPostBreakChoice();
        }
    }

    /** 用户提前结束休息，返回空闲学习态。 */
    private void endBreakEarly() {
        if (sessionType != 1) {
            return;
        }
        cancelPauseTimeoutCheck();
        stopTimerLoop();
        isRunning = false;
        isPaused = false;
        isLongBreak = false;
        awaitingPostBreakChoice = false;
        sessionType = 0;
        timeLeftInMillis = getDefaultStudyTimeMs();
        clearCheckpoint();
        publishState();
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    /** Alarm 触发的到点完成：Service 被杀或 Doze 期间 Handler 未执行时的兜底。 */
    private void handleSessionCompleteAlarm() {
        if (!restoredFromCheckpoint && ActiveSessionStore.hasActiveSession(this)) {
            restoreFromCheckpoint();
        }
        if (!isRunning) {
            return;
        }
        long remaining = timerEndElapsedRealtime - SystemClock.elapsedRealtime();
        if (remaining <= 0L) {
            AppLog.d(TAG, "Session complete via AlarmManager");
            timeLeftInMillis = 0L;
            onTimerComplete();
        } else {
            // 时钟偏差或提前触发：重新注册 Alarm
            TimerAlarmScheduler.scheduleSessionComplete(this, timerEndElapsedRealtime);
        }
    }

    private void openTimerForBreakSession() {
        try {
            Intent intent = new Intent(this, TimerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (currentTaskTitle != null && !currentTaskTitle.isEmpty()) {
                intent.putExtra(EXTRA_TASK_TITLE, currentTaskTitle);
            }
            if (currentTaskId >= 0) {
                intent.putExtra(EXTRA_TASK_ID, currentTaskId);
                intent.putExtra(EXTRA_TASK_CATEGORY, currentCategory);
                intent.putExtra(EXTRA_TASK_TAGS, currentTags);
            }
            startActivity(intent);
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to open timer for break session", e);
        }
    }

    private void openTimerForPostBreakChoice() {
        try {
            Intent intent = new Intent(this, TimerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(EXTRA_POST_BREAK, true);
            if (currentTaskTitle != null && !currentTaskTitle.isEmpty()) {
                intent.putExtra(EXTRA_TASK_TITLE, currentTaskTitle);
            }
            if (currentTaskId >= 0) {
                intent.putExtra(EXTRA_TASK_ID, currentTaskId);
                intent.putExtra(EXTRA_TASK_CATEGORY, currentCategory);
                intent.putExtra(EXTRA_TASK_TAGS, currentTags);
            }
            startActivity(intent);
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to open timer after break", e);
        }
    }

    private void pauseTimer() {
        if (!isRunning || sessionType != 0) {
            return;
        }

        int maxPauseCount = timerSettingsRepository.getMaxPauseCount();
        if (pauseCount >= maxPauseCount) {
            AppLog.d(TAG, "Maximum pause count reached: " + maxPauseCount);
            return;
        }

        pauseSessionInternal(false, null);
        pauseCount++;
    }

    private void pauseTimerWithReason(String reason) {
        if (!isRunning || sessionType != 0) {
            return;
        }

        int maxPauseCount = timerSettingsRepository.getMaxPauseCount();
        if (pauseCount >= maxPauseCount) {
            AppLog.d(TAG, "Maximum pause count reached: " + maxPauseCount);
            return;
        }

        pauseSessionInternal(true, reason);
        pauseCount++;
    }

    private void pauseSessionInternal(boolean recordReason, String reason) {
        stopTimerLoop();
        timeLeftInMillis = Math.max(0L, timerEndElapsedRealtime - SystemClock.elapsedRealtime());
        isRunning = false;
        isPaused = true;
        pauseStartElapsedRealtime = SystemClock.elapsedRealtime();

        if (recordReason && sessionType == 0) {
            long duration = System.currentTimeMillis() - sessionStartTime;
            String userId = AccountManager.getInstance(this).requireActiveUserId();
            List<String> reasons = new ArrayList<>(sessionPauseReasons);
            if (reason != null && !reason.isEmpty()) {
                reasons.add(reason);
            }
            statisticsRepository.recordPausedSession(
                    userId, sessionStartTime, duration, reason, currentTaskId, currentCategory,
                    pauseCount + 1, reasons, true, null);
            if (reason != null && !reason.isEmpty()) {
                sessionPauseReasons.add(reason);
            }
        }

        startPauseTimeoutCheck();
        updateNotification();
        stopForeground(STOP_FOREGROUND_DETACH);
        saveCheckpoint();
        notifyStateChanged();
    }

    private void startPauseTimeoutCheck() {
        cancelPauseTimeoutCheck();

        long remaining = PAUSE_TIMEOUT;
        if (pauseStartElapsedRealtime > 0L) {
            remaining = PAUSE_TIMEOUT - (SystemClock.elapsedRealtime() - pauseStartElapsedRealtime);
            if (remaining <= 0L) {
                endSessionDueToTimeout();
                return;
            }
        }

        final long pauseTimeoutRemaining = remaining;
        pauseTimeoutRunnable = () -> {
            if (isPaused && !isRunning && pauseStartElapsedRealtime > 0L) {
                long pauseDuration = SystemClock.elapsedRealtime() - pauseStartElapsedRealtime;
                if (pauseDuration >= PAUSE_TIMEOUT) {
                    AppLog.d(TAG, "Pause timeout reached via Handler");
                    endSessionDueToTimeout();
                }
            }
        };
        pauseTimeoutHandler.postDelayed(pauseTimeoutRunnable, pauseTimeoutRemaining);
        TimerAlarmScheduler.schedulePauseTimeout(
                this,
                SystemClock.elapsedRealtime() + pauseTimeoutRemaining
        );
    }

    private void cancelPauseTimeoutCheck() {
        if (pauseTimeoutHandler != null && pauseTimeoutRunnable != null) {
            pauseTimeoutHandler.removeCallbacks(pauseTimeoutRunnable);
        }
        pauseTimeoutRunnable = null;
        TimerAlarmScheduler.cancelPauseTimeout(this);
    }

    private void handlePauseTimeoutAlarm() {
        if (!restoredFromCheckpoint && ActiveSessionStore.hasActiveSession(this)) {
            restoreFromCheckpoint();
        }
        if (isPaused && !isRunning && pauseStartElapsedRealtime > 0L) {
            long pauseDuration = SystemClock.elapsedRealtime() - pauseStartElapsedRealtime;
            if (pauseDuration >= PAUSE_TIMEOUT) {
                AppLog.d(TAG, "Pause timeout reached via AlarmManager");
                endSessionDueToTimeout();
            }
        }
    }

    private void endSessionDueToTimeout() {
        cancelPauseTimeoutCheck();
        stopTimerLoop();
        isRunning = false;
        isPaused = false;
        timeLeftInMillis = 0L;
        clearCheckpoint();

        if (sessionType == 0) {
            long duration = System.currentTimeMillis() - sessionStartTime;
            String userId = AccountManager.getInstance(this).requireActiveUserId();
            List<String> reasons = new ArrayList<>(sessionPauseReasons);
            String pauseReasonTimeout = getString(R.string.timer_pause_reason_timeout);
            reasons.add(pauseReasonTimeout);
            statisticsRepository.recordPausedSession(
                    userId, sessionStartTime, duration, pauseReasonTimeout, currentTaskId, currentCategory,
                    Math.max(pauseCount, 1), reasons, true, null);
            stopAppBlockingService();
            FocusDndHelper.restoreDnd(this);
        }

        notifyTimerFinish();

        sendFailedNotification();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void endSessionDueToLeave() {
        cancelPauseTimeoutCheck();
        stopTimerLoop();
        isRunning = false;
        isPaused = false;
        timeLeftInMillis = 0L;
        clearCheckpoint();

        if (sessionType == 0) {
            stopAppBlockingService();
            FocusDndHelper.restoreDnd(this);
        }

        notifyTimerFinish();

        sendFailedNotification();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    public void resetTimer() {
        cancelPauseTimeoutCheck();
        stopTimerLoop();
        boolean wasStudy = sessionType == 0;
        isRunning = false;
        isPaused = false;
        pauseCount = 0;
        sessionPauseReasons.clear();
        pauseStartElapsedRealtime = 0L;
        plannedDurationMs = 0L;
        timeLeftInMillis = getDefaultStudyTimeMs();
        sessionType = 0;
        isLongBreak = false;
        awaitingPostBreakChoice = false;
        clearCurrentTask();
        clearCheckpoint();

        if (wasStudy) {
            stopAppBlockingService();
            FocusDndHelper.restoreDnd(this);
        }

        notifyTimerTick(timeLeftInMillis);
        notifyTimerReset();
        notifyStateChanged();

        publishState();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    /**
     * 使用 specialUse 前台服务类型，支持长时间番茄钟，避免 shortService 3 分钟超时 ANR。
     */
    private void startForegroundWithNotification() {
        enterForeground(createNotification());
    }

    private void enterForeground(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                );
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    /** 服务已启动但无活跃计时时的占位通知（满足 startForeground 时限）。 */
    private Notification createIdleNotification() {
        return buildBaseNotificationBuilder()
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.timer_notification_ready))
                .setOngoing(false)
                .build();
    }

    /** 存在未恢复快照、等待用户在应用内确认时的占位通知。 */
    private Notification createPendingRecoveryNotification() {
        return buildBaseNotificationBuilder()
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.timer_notification_recovery_prompt))
                .setOngoing(false)
                .build();
    }

    private NotificationCompat.Builder buildBaseNotificationBuilder() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false);
    }

    /** 将会话快照写入磁盘，供进程回收后恢复。 */
    private void saveCheckpoint() {
        if (!isRunning && !isPaused) {
            return;
        }
        ActiveSessionStore.save(
                this,
                AccountManager.getInstance(this).requireActiveUserId(),
                timerEndElapsedRealtime,
                getTimeLeft(),
                isRunning,
                isPaused,
                sessionType,
                sessionStartTime,
                pauseCount,
                pauseStartElapsedRealtime,
                plannedDurationMs,
                currentTaskId,
                currentSubTaskId,
                currentTaskTitle,
                currentCategory,
                currentTags,
                SessionPauseUtils.encodeReasons(sessionPauseReasons),
                isLongBreak,
                awaitingPostBreakChoice
        );
    }

    private void maybeSaveCheckpointThrottled() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastCheckpointSaveElapsed >= CHECKPOINT_SAVE_INTERVAL_MS) {
            lastCheckpointSaveElapsed = now;
            saveCheckpoint();
        }
    }

    private void clearCheckpoint() {
        ActiveSessionStore.clear(this);
        TimerAlarmScheduler.cancelAll(this);
    }

    private void notifyStateChanged() {
        publishState();
        saveCheckpoint();
        for (TimerListener listener : listeners) {
            listener.onTimerStateChanged();
        }
    }

    private void notifyTimerTick(long millis) {
        publishState();
        for (TimerListener listener : listeners) {
            listener.onTimerTick(millis);
        }
    }

    private void notifyTimerFinish() {
        publishState();
        for (TimerListener listener : listeners) {
            listener.onTimerFinish();
        }
    }

    private void notifyTimerReset() {
        publishState();
        for (TimerListener listener : listeners) {
            listener.onTimerReset();
        }
    }

    private void playAlarm() {
        try {
            String ringtoneUri = settingsManager.getRingtoneUri();
            if ("silent".equals(ringtoneUri)) {
                return;
            }

            Uri uri = "default".equals(ringtoneUri)
                    ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    : Uri.parse(ringtoneUri);

            if (uri == null) {
                return;
            }

            Context audioContext = getAudioAttributionContext();
            MediaPlayer player = MediaPlayer.create(audioContext, uri);
            if (player == null) {
                return;
            }
            player.setOnCompletionListener(MediaPlayer::release);
            player.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                return true;
            });
            player.start();
        } catch (Exception e) {
            AppLog.e(TAG, "Error playing alarm", e);
        }
    }

    private Context getAudioAttributionContext() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return createAttributionContext("timerAlarm");
        }
        return this;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) {
                return;
            }

            NotificationChannel ongoingChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.timer_channel_ongoing_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            ongoingChannel.setDescription(getString(R.string.timer_channel_ongoing_description));
            ongoingChannel.enableVibration(false);
            ongoingChannel.setSound(null, null);
            ongoingChannel.setShowBadge(false);
            ongoingChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(ongoingChannel);

            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    getString(R.string.timer_channel_alert_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription(getString(R.string.timer_channel_alert_description));
            alertChannel.enableVibration(true);
            alertChannel.setShowBadge(true);
            alertChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(alertChannel);
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, TimerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        String title;
        if (sessionType == 0) {
            title = getString(R.string.timer_notification_title_study);
        } else if (isLongBreak) {
            title = getString(R.string.timer_notification_title_long_break);
        } else {
            title = getString(R.string.timer_notification_title_break);
        }
        NotificationCompat.Builder builder = buildBaseNotificationBuilder()
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setOngoing(isRunning || isPaused)
                .setAutoCancel(false)
                .setSilent(true);

        if (isRunning) {
            // contentText 显式展示剩余时间；Chronometer 作为锁屏平滑倒计时的补充
            long endWallClock = System.currentTimeMillis() + getTimeLeft();
            builder.setUsesChronometer(true);
            builder.setChronometerCountDown(true);
            builder.setShowWhen(true);
            builder.setWhen(endWallClock);
            builder.setContentText(getString(R.string.timer_notification_remaining, getTimerText()));
        } else if (isPaused) {
            builder.setContentText(getString(R.string.timer_notification_paused_remaining, getTimerText()));
        } else {
            builder.setContentText(sessionType == 0
                    ? getString(R.string.timer_notification_study_time, getTimerText())
                    : getString(R.string.timer_notification_break_time, getTimerText()));
        }
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (sessionType == 0 && (isRunning || isPaused)) {
            if (isRunning && canPause()) {
                builder.addAction(R.drawable.ic_pause,
                        getString(R.string.pause_button),
                        buildServiceActionPendingIntent(ACTION_PAUSE, 20));
            } else if (isPaused) {
                builder.addAction(R.drawable.ic_resume,
                        getString(R.string.resume_button),
                        buildServiceActionPendingIntent(ACTION_RESUME, 21));
            }
            builder.addAction(R.drawable.ic_stop,
                    getString(R.string.stop_button),
                    buildServiceActionPendingIntent(ACTION_RESET, 22));
        }

        return builder.build();
    }

    private void updateNotification() {
        Notification notification = createNotification();
        if (isRunning) {
            // 运行中通过 startForeground 刷新，确保锁屏 Chronometer 持续更新
            startForegroundWithNotification();
            return;
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void sendFailedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
                .setContentTitle(getString(R.string.timer_notification_fail_title))
                .setContentText(getString(R.string.timer_notification_fail_message))
                .setSmallIcon(R.drawable.ic_timer)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            manager.notify(NOTIFICATION_ID + 2, builder.build());
        }
    }

    private void notifyOrVibrate(NotificationCompat.Builder builder, int notificationId) {
        applyCompletionAlertPolicy(builder);
        if (NotificationPermission.hasNotificationPermission(this)) {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build());
        } else {
            playAlarm();
            VibrationHelper.vibrateAlert(this);
        }
    }

    /**
     * 到点提醒策略：有通知权限时由通知渠道负责声音/振动；
     * 无权限时由 {@link #notifyOrVibrate} 走铃声 + 震动，避免重复提醒被系统判为 noisy 而静音。
     */
    private void applyCompletionAlertPolicy(NotificationCompat.Builder builder) {
        builder.setOnlyAlertOnce(true);
        if ("silent".equals(settingsManager.getRingtoneUri())) {
            builder.setSilent(true);
        }
    }

    private void sendBreakStartedNotification() {
        String title = isLongBreak
                ? getString(R.string.timer_notification_study_complete_long_break_title)
                : getString(R.string.timer_notification_study_complete_break_title);
        String text = isLongBreak
                ? getString(R.string.timer_notification_long_break_text)
                : getString(R.string.timer_notification_short_break_text);

        Intent openTimer = new Intent(this, TimerActivity.class);
        openTimer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int openFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            openFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openTimer, openFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_timer)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent);

        notifyOrVibrate(builder, NOTIFICATION_ID + 1);
    }

    private NotificationCompat.Builder buildAlertNotificationBuilder() {
        return new NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
                .setSmallIcon(R.drawable.ic_timer)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    private void sendBreakEndedNotification() {
        Intent openTimer = new Intent(this, TimerActivity.class);
        openTimer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openTimer.putExtra(EXTRA_POST_BREAK, true);
        int openFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            openFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openTimer, openFlags);
        PendingIntent startPendingIntent = buildServiceActionPendingIntent(ACTION_START, 12);
        PendingIntent resetPendingIntent = buildServiceActionPendingIntent(ACTION_RESET, 13);

        NotificationCompat.Builder builder = buildAlertNotificationBuilder()
                .setContentTitle(getString(R.string.timer_notification_break_end_title))
                .setContentText(getString(R.string.timer_notification_break_end_message))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_study, getString(R.string.timer_notification_action_start_next), startPendingIntent)
                .addAction(R.drawable.ic_stop, getString(R.string.timer_end_session), resetPendingIntent);

        notifyOrVibrate(builder, NOTIFICATION_ID + 3);
    }

    private void sendNextPomodoroStartedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.timer_notification_break_end_title))
                .setContentText(getString(R.string.timer_notification_break_end_auto))
                .setSmallIcon(R.drawable.ic_timer)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true);

        if (NotificationPermission.hasNotificationPermission(this)) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID + 3, builder.build());
        }
    }

    private void sendActivityEndedBroadcast() {
        sendBroadcast(new Intent(ACTION_ACTIVITY_ENDED_BROADCAST));
    }

    private PendingIntent buildServiceActionPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, TimerService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private String getTimerText() {
        long millis = getTimeLeft();
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // 用户划掉最近任务时落盘，便于下次启动恢复
        saveCheckpoint();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (isRunning || isPaused) {
            saveCheckpoint();
        }
        cancelPauseTimeoutCheck();
        stopTimerLoop();
        listeners.clear();

        // 进行中会话保留通知，便于用户返回
        if (!isRunning && !isPaused) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            manager.cancel(NOTIFICATION_ID);
            manager.cancel(NOTIFICATION_ID + 1);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void addListener(TimerListener listener) {
        if (listener == null) {
            return;
        }
        listeners.addIfAbsent(listener);
        listener.onTimerTick(getTimeLeft());
        listener.onTimerStateChanged();
        publishState();
    }

    public void removeListener(TimerListener listener) {
        listeners.remove(listener);
    }

    /** @deprecated 请使用 {@link #addListener(TimerListener)} / {@link #removeListener(TimerListener)} */
    @Deprecated
    public void setListener(TimerListener listener) {
        listeners.clear();
        if (listener != null) {
            addListener(listener);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public int getPauseCount() {
        return pauseCount;
    }

    public int getRemainingPauseCount() {
        int maxPauseCount = timerSettingsRepository.getMaxPauseCount();
        return Math.max(0, maxPauseCount - pauseCount);
    }

    public boolean canPause() {
        return sessionType == 0 && pauseCount < timerSettingsRepository.getMaxPauseCount();
    }

    public long getTimeLeft() {
        if (isRunning) {
            return Math.max(0L, timerEndElapsedRealtime - SystemClock.elapsedRealtime());
        }
        return timeLeftInMillis;
    }

    public int getSessionType() {
        return sessionType;
    }

    public boolean isLongBreak() {
        return isLongBreak;
    }

    public boolean isAwaitingPostBreakChoice() {
        return awaitingPostBreakChoice;
    }

    public void setCurrentTask(int taskId, String category, String tags) {
        setCurrentTask(taskId, null, category, tags, -1);
    }

    public void setCurrentTask(int taskId, String title, String category, String tags) {
        setCurrentTask(taskId, title, category, tags, -1);
    }

    public void setCurrentTask(int taskId, String title, String category, String tags, int subTaskId) {
        this.currentTaskId = taskId;
        this.currentSubTaskId = subTaskId;
        this.currentTaskTitle = title != null ? title : "";
        this.currentCategory = category != null ? category : CategoryDefaults.getDefault();
        this.currentTags = tags != null ? tags : "";
        saveCheckpoint();
    }

    public void clearCurrentTask() {
        currentTaskId = -1;
        currentSubTaskId = -1;
        currentTaskTitle = "";
        currentCategory = CategoryDefaults.getDefault();
        currentTags = "";
    }

    public String getCurrentTaskTitle() {
        return currentTaskTitle;
    }

    public int getCurrentTaskId() {
        return currentTaskId;
    }

    private void startAppBlockingService() {
        Intent intent = new Intent(this, AppBlockingService.class);
        intent.putExtra("action", "start_blocking");
        startService(intent);
    }

    private void stopAppBlockingService() {
        Intent intent = new Intent(this, AppBlockingService.class);
        intent.putExtra("action", "stop_blocking");
        startService(intent);
    }

    private void incrementTaskCompletedPomodoros(int taskId) {
        AppExecutors.getInstance().diskIo(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            if (currentSubTaskId >= 0) {
                db.subTaskDao().incrementCompletedPomodoros(currentSubTaskId);
            } else {
                TodoItem task = db.todoDao().getTodoByIdSync(taskId);
                if (task != null && task.isSimple()) {
                    db.todoDao().incrementCompletedPomodoros(taskId);
                }
            }
        });
    }
}
