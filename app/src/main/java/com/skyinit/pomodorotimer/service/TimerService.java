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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.app.ActivityManager;
import androidx.core.content.ContextCompat;
import android.media.AudioAttributes;
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
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.CategoryDefaults;
import com.skyinit.pomodorotimer.util.FocusDndHelper;
import com.skyinit.pomodorotimer.data.repository.SessionBlockRecordRepository;
import com.skyinit.pomodorotimer.util.AppBlockingServiceUtils;
import com.skyinit.pomodorotimer.util.LockScreenPresenter;
import com.skyinit.pomodorotimer.util.PermissionUtils;
import com.skyinit.pomodorotimer.util.SessionPauseUtils;
import com.skyinit.pomodorotimer.domain.timer.SessionPhase;
import com.skyinit.pomodorotimer.domain.timer.SettleDecision;
import com.skyinit.pomodorotimer.domain.timer.TimerSessionPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * 番茄钟核心前台服务：驱动学习/休息计时、阶段切换、暂停超时判定与统计写入。
 * <p>
 * 状态流向：Service → {@link com.skyinit.pomodorotimer.data.repository.TimerStateRepository}
 * → ViewModel → UI。运行中定期将快照写入 {@link com.skyinit.pomodorotimer.data.repository.ActiveSessionStore}，
 * 并调度 Alarm 兜底；学习阶段可联动启动 {@link AppBlockingService}。
 */
public class TimerService extends Service {
    private static final String TAG = "TimerService";
    private static final String CHANNEL_ID = "TimerChannelOngoing";
    private static final String CHANNEL_ID_ALERTS = "TimerAlertChannel";
    /** 前台进行中计时通知（静默，仅展示进度）。 */
    private static final int NOTIFICATION_ID_ONGOING = 1;
    /** 阶段切换提醒通知 ID，彼此独立避免覆盖。 */
    private static final int NOTIFICATION_ID_ALERT_BREAK_STARTED = 2;
    private static final int NOTIFICATION_ID_ALERT_STUDY_STARTED = 3;
    private static final int NOTIFICATION_ID_ALERT_BREAK_ENDED = 4;
    private static final int NOTIFICATION_ID_ALERT_STUDY_AUTO_STARTED = 5;
    private static final int NOTIFICATION_ID_ALERT_FAILED = 6;
    /** 连续提醒之间的间隔，避免通知与铃声互相覆盖。 */
    private static final long ALERT_SEQUENCE_GAP_MS = 800L;
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
    public static final String ACTION_ACCOUNT_SWITCH_RESET = "ACCOUNT_SWITCH_RESET";
    public static final String ACTION_PAUSE_WITH_REASON = "PAUSE_WITH_REASON";
    public static final String ACTION_PAUSE_TIMEOUT = "PAUSE_TIMEOUT";
    /** Alarm 触发的会话到点完成（进程被杀后的兜底入口）。 */
    public static final String ACTION_SESSION_COMPLETE = "SESSION_COMPLETE";
    /** 从快照恢复或评估结算。 */
    public static final String ACTION_RESTORE_SESSION = "RESTORE_SESSION";
    /** 统一评估磁盘快照：未到期恢复，已到期结算。 */
    public static final String ACTION_EVALUATE_CHECKPOINT = "EVALUATE_CHECKPOINT";
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
    private SessionBlockRecordRepository sessionBlockRecordRepository;
    private SettingsManager settingsManager;
    private long sessionStartTime;
    /** 会话归属账户在开始/恢复时固定，统计写入不得使用切换后的当前账户。 */
    private String sessionUserId = "";
    private int currentTaskId = -1;
    private int currentSubTaskId = -1;
    private String currentTaskTitle = "";
    private String currentCategory = CategoryDefaults.getDefault();
    private String currentTags = "";

    private String getSessionUserIdOrActive() {
        if (sessionUserId != null && !sessionUserId.isEmpty()) {
            return sessionUserId;
        }
        String activeUserId = AccountManager.getInstance(this).getCurrentUserId();
        if (activeUserId == null || activeUserId.isEmpty()) {
            throw new IllegalStateException("No active registered session");
        }
        sessionUserId = activeUserId;
        return sessionUserId;
    }

    /** 新轮次开始时绑定会话归属；若内存中残留其他账户 ID 则纠正为当前活跃账户。 */
    private void bindSessionUserForNewSession() {
        String activeUserId = AccountManager.getInstance(this).getCurrentUserId();
        if (activeUserId == null || activeUserId.isEmpty()) {
            throw new IllegalStateException("No active registered session");
        }
        if (sessionUserId == null || sessionUserId.isEmpty()) {
            sessionUserId = activeUserId;
        } else if (!activeUserId.equals(sessionUserId)) {
            AppLog.w(TAG, "Rebinding stale session user " + sessionUserId + " to " + activeUserId);
            sessionUserId = activeUserId;
        }
    }

    private int pauseCount;
    private final List<String> sessionPauseReasons = new ArrayList<>();
    private long pauseStartElapsedRealtime;
    private long plannedDurationMs;
    private long lastCheckpointSaveElapsed;
    /** 标记是否已从磁盘恢复，避免 onCreate 与 onStartCommand 重复恢复。 */
    private boolean restoredFromCheckpoint;
    /** 防止 Alarm 恢复与 handleSessionCompleteAlarm 重复触发完成逻辑。 */
    private final AtomicBoolean isCompletingSession = new AtomicBoolean(false);
    /** Alarm / checkpoint 代际，防止过期触发误结算。 */
    private int sessionGeneration;
    /** 上次已同步到提醒渠道的铃声 URI，避免重复写渠道。 */
    private String syncedAlertRingtoneUri = "";

    private LockScreenPresenter lockScreenPresenter;
    private boolean lockScreenReceiverRegistered;
    private final BroadcastReceiver lockScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || lockScreenPresenter == null) {
                return;
            }
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                lockScreenPresenter.onScreenOff();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                lockScreenPresenter.onScreenOn();
            } else if (Intent.ACTION_USER_UNLOCKED.equals(action)
                    || Intent.ACTION_USER_PRESENT.equals(action)) {
                lockScreenPresenter.onUserUnlocked();
            }
        }
    };

    /** 需要响铃提醒的计时阶段切换节点。 */
    private enum SessionAlert {
        STUDY_STARTED,
        BREAK_STARTED,
        BREAK_ENDED,
        SESSION_FAILED
    }

    /** 供 TimerService 绑定 Activity 时获取服务实例。 */
    public class LocalBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }

    /** 计时事件回调（保留接口，UI 已改由 LiveData 驱动）。 */
    public interface TimerListener {
        void onTimerTick(long millisUntilFinished);
        void onTimerFinish();
        void onTimerReset();
        void onTimerStateChanged();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Alarm 唤醒进程时可能未走完 App 业务 init：幂等孤儿勿扰兜底（有活跃会话则跳过）
        FocusDndHelper.recoverOrphanIfNeeded(this);
        createNotificationChannel();
        AppContainer container = AppContainer.getInstance(this);
        timerSettingsRepository = container.getTimerSettingsRepository();
        statisticsRepository = container.getStatisticsRepository();
        timerStateRepository = container.getTimerStateRepository();
        pomodoroSettingsRepository = container.getUserPomodoroSettingsRepository();
        sessionBlockRecordRepository = container.getSessionBlockRecordRepository();
        pomodoroSettingsRepository.warmCache();
        AppExecutors.getInstance().diskIo(() -> {
            String userId = AccountManager.getInstance(TimerService.this).getCurrentUserId();
            container.getUserAppBlockingRepository().warmForUser(userId);
        });
        timeLeftInMillis = timerSettingsRepository.getDefaultStudyTimeMs();
        settingsManager = container.getSettingsManager();
        lockScreenPresenter = new LockScreenPresenter(this);
        syncAlertChannelSound();
        timerHandler = new Handler(Looper.getMainLooper());
        pauseTimeoutHandler = new Handler(Looper.getMainLooper());
        tickRunnable = this::onTick;
        // 有快照时保留 Alarm，不在此处 cancelAll（否则拆掉进程死后兜底）
        publishState();
        syncLockScreenPresenter();
    }

    private long getDefaultStudyTimeMs() {
        return timerSettingsRepository.getDefaultStudyTimeMs();
    }

    private long getBreakTimeMs() {
        return timerSettingsRepository.getDefaultBreakTimeMs();
    }

    private void publishState() {
        long pauseRemaining = 0L;
        if (isPaused && pauseStartElapsedRealtime > 0L) {
            long pauseElapsed = TimerSessionPolicy.resolvePauseElapsed(
                    pauseStartElapsedRealtime,
                    0L,
                    SystemClock.elapsedRealtime(),
                    System.currentTimeMillis());
            pauseRemaining = Math.max(0L, PAUSE_TIMEOUT - pauseElapsed);
        }
        timerStateRepository.publish(
                getTimeLeft(),
                isRunning,
                isPaused,
                sessionType,
                awaitingPostBreakChoice,
                isLongBreak,
                sessionStartTime,
                sessionGeneration,
                pauseRemaining,
                ExactAlarmPermissionHelperCanSchedule(),
                canPause());
        syncLockScreenPresenter();
    }

    private void syncLockScreenPresenter() {
        if (lockScreenPresenter == null || timerSettingsRepository == null) {
            return;
        }
        boolean pref = timerSettingsRepository.isLockScreenFullscreenEnabled();
        boolean eligible = isRunning || isPaused;
        lockScreenPresenter.updateSession(pref, isRunning, isPaused);
        if (pref && eligible) {
            ensureLockScreenReceiverRegistered();
        } else {
            unregisterLockScreenReceiver();
        }
    }

    private void ensureLockScreenReceiverRegistered() {
        if (lockScreenReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            filter.addAction(Intent.ACTION_USER_UNLOCKED);
        }
        // 系统广播需 EXPORTED，否则部分机型收不到 USER_PRESENT/USER_UNLOCKED，
        // 导致长按解锁后 suppress 无法清除、再次亮屏不再全屏展示。
        ContextCompat.registerReceiver(this, lockScreenReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        lockScreenReceiverRegistered = true;
    }

    private void unregisterLockScreenReceiver() {
        if (!lockScreenReceiverRegistered) {
            return;
        }
        try {
            unregisterReceiver(lockScreenReceiver);
        } catch (Exception ignored) {
        }
        lockScreenReceiverRegistered = false;
    }

    private boolean ExactAlarmPermissionHelperCanSchedule() {
        return com.skyinit.pomodorotimer.util.ExactAlarmPermissionHelper.canScheduleExactAlarms(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // startForegroundService() 要求在约 5 秒内调用 startForeground()，否则系统 ANR
        promoteToForegroundImmediately();

        // 空启动 / START_STICKY：静默恢复未到期会话，到期则交由 Alarm 或此处结算
        if (intent == null || intent.getAction() == null) {
            trySilentRestoreOrSettle(-1);
            downgradeForegroundIfIdle();
            return START_STICKY;
        }

        if (intent != null) {
            String action = intent.getAction();
            int alarmGeneration = intent.getIntExtra(TimerAlarmScheduler.EXTRA_GENERATION, -1);
            if (ACTION_START.equals(action)) {
                if (!AccountManager.getInstance(this).hasActiveSession()) {
                    AppLog.w(TAG, "Refuse ACTION_START without registered session");
                    downgradeForegroundIfIdle();
                    return START_NOT_STICKY;
                }
                // 进行中（含暂停）会话禁止被 START 重置，否则会清零 pauseCount，绕过暂停上限
                if (isRunning || isPaused) {
                    AppLog.w(TAG, "Ignore ACTION_START while session active"
                            + " (running=" + isRunning + ", paused=" + isPaused + ")");
                    startForegroundWithNotification();
                    return START_STICKY;
                }
                awaitingPostBreakChoice = false;
                long customStudyMs = intent.getLongExtra(EXTRA_STUDY_DURATION_MS, -1L);
                if (sessionType == 1 && timeLeftInMillis > 0L) {
                    isLongBreak = false;
                    timeLeftInMillis = getBreakTimeMs();
                    beginNewSession();
                    dispatchSessionAlert(SessionAlert.BREAK_STARTED);
                } else {
                    sessionType = 0;
                    isLongBreak = false;
                    if (customStudyMs > 0L) {
                        timeLeftInMillis = TimerSettingsRepository.clampStudy(customStudyMs);
                    } else {
                        timeLeftInMillis = getDefaultStudyTimeMs();
                    }
                    beginNewSession();
                    dispatchSessionAlert(SessionAlert.STUDY_STARTED, false);
                }
            } else if (ACTION_END_BREAK.equals(action)) {
                endBreakEarly();
            } else if (ACTION_PAUSE.equals(action)) {
                pauseTimer();
            } else if (ACTION_RESUME.equals(action)) {
                resumeTimer();
            } else if (ACTION_RESET.equals(action)) {
                resetTimer();
            } else if (ACTION_ACCOUNT_SWITCH_RESET.equals(action)) {
                resetTimerForAccountSwitch();
            } else if (ACTION_PAUSE_WITH_REASON.equals(action)) {
                pauseTimerWithReason(intent.getStringExtra("pause_reason"));
            } else if (ACTION_FORCE_FAIL.equals(action)) {
                endSessionDueToLeave();
            } else if (ACTION_PAUSE_TIMEOUT.equals(action)) {
                handlePauseTimeoutAlarm(alarmGeneration);
            } else if (ACTION_SESSION_COMPLETE.equals(action)) {
                handleSessionCompleteAlarm(alarmGeneration);
            } else if (ACTION_RESTORE_SESSION.equals(action)
                    || ACTION_EVALUATE_CHECKPOINT.equals(action)) {
                trySilentRestoreOrSettle(alarmGeneration);
            }
        }

        // 空闲时降为后台服务；暂停态保持轻量 FGS，避免被杀后丢 Handler
        downgradeForegroundIfIdle();
        return START_STICKY;
    }

    /**
     * 静默恢复未到期会话；若已到期则按 1-A 自动结算。
     */
    private void trySilentRestoreOrSettle(int alarmGeneration) {
        if (isRunning || isPaused || awaitingPostBreakChoice || restoredFromCheckpoint) {
            return;
        }
        if (!ActiveSessionStore.hasActiveSession(this)) {
            return;
        }
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(this);
        if (cp == null) {
            return;
        }
        if (cp.running) {
            long remaining = ActiveSessionStore.computeRemainingMillis(cp);
            restoreFromCheckpoint();
            if (remaining <= 0L && isRunning) {
                timeLeftInMillis = 0L;
                onTimerComplete();
            }
        } else if (cp.paused) {
            long pauseElapsed = ActiveSessionStore.computePauseElapsedMillis(cp);
            restoreFromCheckpoint();
            if (pauseElapsed >= PAUSE_TIMEOUT) {
                endSessionDueToTimeout();
            }
        } else if (cp.awaitingPostBreakChoice) {
            restoreFromCheckpoint();
        } else {
            restoreFromCheckpoint();
        }
    }

    /**
     * 立即进入前台状态，满足 startForegroundService 的时限要求。
     * 空闲时使用轻量通知，运行/暂停时使用计时通知。
     */
    private void promoteToForegroundImmediately() {
        Notification notification;
        if (isRunning || isPaused) {
            notification = createNotification();
        } else if (ActiveSessionStore.hasActiveSession(this)) {
            notification = createNotification();
        } else {
            notification = createIdleNotification();
        }
        enterForeground(notification);
    }

    /** 非运行且非暂停时降回后台。暂停保持 FGS。 */
    private void downgradeForegroundIfIdle() {
        if (!isRunning && !isPaused) {
            stopForeground(STOP_FOREGROUND_DETACH);
        }
    }

    /**
     * 从 ActiveSessionStore 恢复会话。若计时已过期则立即完成；若暂停已超时则判定失败。
     *
     * @return true 表示已进入活跃态（运行/暂停/待选择）或已触发结算；false 表示无快照或因账户边界丢弃
     */
    private boolean restoreFromCheckpoint() {
        if (isRunning || isPaused || awaitingPostBreakChoice) {
            restoredFromCheckpoint = true;
            return true;
        }
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(this);
        if (cp == null) {
            return false;
        }

        String activeUserId = AccountManager.getInstance(this).getCurrentUserId();
        if (activeUserId == null || activeUserId.isEmpty() || !cp.belongsToUser(activeUserId)) {
            AppLog.w(TAG, "Discarding checkpoint for inactive/mismatched user: " + cp.userId);
            clearCheckpoint();
            resetTimerQuietly();
            return false;
        }

        sessionUserId = cp.userId;
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
            long remaining = ActiveSessionStore.computeRemainingMillis(cp);
            sessionGeneration = cp.generation;
            if (remaining <= 0L) {
                // 必须先置为 false，否则 handleSessionCompleteAlarm 会再次调用 onTimerComplete
                isRunning = false;
                isPaused = false;
                timeLeftInMillis = 0L;
                restoredFromCheckpoint = true;
                onTimerComplete();
                return true;
            }
            isRunning = true;
            isPaused = false;
            timeLeftInMillis = remaining;
            // 用当前剩余重建终点（兼容重启后 wall 解析）
            timerEndElapsedRealtime = SystemClock.elapsedRealtime() + remaining;
            restoredFromCheckpoint = true;
            startForegroundWithNotification();
            startTimerLoop();
            if (sessionType == 0 && isRunning && !isPaused) {
                maybeStartTimerBlocking();
            }
            notifyStateChanged();
            return true;
        } else if (cp.paused) {
            isPaused = true;
            isRunning = false;
            timeLeftInMillis = cp.timeLeftInMillis;
            pauseStartElapsedRealtime = cp.pauseStartElapsedRealtime;
            sessionGeneration = cp.generation;
            long pauseElapsed = ActiveSessionStore.computePauseElapsedMillis(cp);
            restoredFromCheckpoint = true;
            if (pauseElapsed >= PAUSE_TIMEOUT) {
                timerHandler.post(this::endSessionDueToTimeout);
                return true;
            }
            // 校正暂停起点为“剩余超时”对应的 elapsed
            pauseStartElapsedRealtime = SystemClock.elapsedRealtime() - pauseElapsed;
            startPauseTimeoutCheck();
            startForegroundWithNotification();
            notifyStateChanged();
            return true;
        } else if (cp.awaitingPostBreakChoice) {
            sessionGeneration = cp.generation;
            timeLeftInMillis = cp.timeLeftInMillis > 0L ? cp.timeLeftInMillis : getDefaultStudyTimeMs();
            restoredFromCheckpoint = true;
            notifyStateChanged();
            return true;
        } else {
            clearCheckpoint();
            resetTimerQuietly();
            return false;
        }
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
        sessionUserId = "";
        clearCurrentTask();
        publishState();
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    /** 全新开始一轮计时（重置暂停次数与会话起点）。 */
    private void beginNewSession() {
        if (isRunning || isPaused) {
            return;
        }

        bindSessionUserForNewSession();
        cancelPauseTimeoutCheck();
        sessionGeneration++;
        isRunning = true;
        isPaused = false;
        pauseCount = 0;
        sessionPauseReasons.clear();
        pauseStartElapsedRealtime = 0L;
        sessionStartTime = System.currentTimeMillis();
        plannedDurationMs = timeLeftInMillis;

        if (sessionType == 0 && isRunning && !isPaused) {
            maybeStartTimerBlocking();
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
        sessionGeneration++;
        isPaused = false;
        pauseStartElapsedRealtime = 0L;
        isRunning = true;

        if (sessionType == 0) {
            statisticsRepository.deletePauseSnapshotsForSession(
                    getSessionUserIdOrActive(), sessionStartTime);
            maybeStartTimerBlocking();
        }

        startForegroundWithNotification();
        startTimerLoop();
        notifyStateChanged();
    }

    private void startTimerLoop() {
        timerEndElapsedRealtime = SystemClock.elapsedRealtime() + timeLeftInMillis;
        timerHandler.removeCallbacks(tickRunnable);
        timerHandler.post(tickRunnable);
        TimerAlarmScheduler.scheduleSessionComplete(this, timerEndElapsedRealtime, sessionGeneration);
    }

    /** 停 Handler 并取消到点 Alarm（用户主动停止/正常结算时使用）。 */
    private void stopTimerLoop() {
        stopHandlerTicksOnly();
        TimerAlarmScheduler.cancelSessionComplete(this);
    }

    /** 仅停 Handler，保留 Alarm（onDestroy 活跃会话时使用）。 */
    private void stopHandlerTicksOnly() {
        if (timerHandler != null && tickRunnable != null) {
            timerHandler.removeCallbacks(tickRunnable);
        }
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
        if (!isCompletingSession.compareAndSet(false, true)) {
            AppLog.d(TAG, "Ignoring duplicate onTimerComplete");
            return;
        }
        try {
            stopTimerLoop();
            isRunning = false;
            isPaused = false;
            cancelPauseTimeoutCheck();
            long settledId = sessionStartTime;
            int completedSessionType = sessionType;

            notifyTimerFinish();
            stopTimerBlocking();
            FocusDndHelper.restoreDnd(this);

            if (completedSessionType == 0) {
                long duration = System.currentTimeMillis() - sessionStartTime;
                String userId = getSessionUserIdOrActive();
                statisticsRepository.recordSession(
                        userId, sessionStartTime, duration, currentTaskId, currentSubTaskId,
                        currentCategory, currentTags,
                        pauseCount, new ArrayList<>(sessionPauseReasons), false, null);
                if (currentTaskId >= 0) {
                    incrementTaskCompletedPomodoros(currentTaskId, userId);
                }

                ActiveSessionStore.markSettledResult(
                        this, settledId, ActiveSessionStore.RESULT_BREAK_STARTED);
                prepareAndAutoStartBreak();
            } else {
                ActiveSessionStore.markSettledResult(
                        this, settledId, ActiveSessionStore.RESULT_COMPLETED);
                handleBreakComplete();
            }
            if (!isRunning && !isPaused) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            }
        } finally {
            isCompletingSession.set(false);
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
        dispatchSessionAlert(SessionAlert.BREAK_STARTED);
        openTimerForBreakSession();
    }

    /** 休息自然结束：按设置自动开始下一轮或等待用户选择。 */
    private void handleBreakComplete() {
        isLongBreak = false;

        UserPomodoroSettings settings = pomodoroSettingsRepository.getSettings();
        if (settings.autoStartAfterBreak) {
            dispatchSessionAlert(SessionAlert.BREAK_ENDED, true);

            awaitingPostBreakChoice = false;
            sessionType = 0;
            timeLeftInMillis = getDefaultStudyTimeMs();
            publishState();
            beginNewSession();

            timerHandler.postDelayed(
                    () -> dispatchSessionAlert(SessionAlert.STUDY_STARTED, true),
                    ALERT_SEQUENCE_GAP_MS);
        } else {
            dispatchSessionAlert(SessionAlert.BREAK_ENDED, false);
            sessionType = 0;
            timeLeftInMillis = getDefaultStudyTimeMs();
            awaitingPostBreakChoice = true;
            saveCheckpoint();
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
        sessionUserId = "";
        clearCheckpoint();
        publishState();
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    /** Alarm 触发的到点完成：Service 被杀或 Doze 期间 Handler 未执行时的兜底。 */
    private void handleSessionCompleteAlarm(int alarmGeneration) {
        if (!restoredFromCheckpoint && ActiveSessionStore.hasActiveSession(this)) {
            ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(this);
            if (cp != null && alarmGeneration >= 0 && alarmGeneration != cp.generation) {
                AppLog.d(TAG, "Ignoring stale session-complete alarm gen=" + alarmGeneration);
                return;
            }
            restoreFromCheckpoint();
        } else if (alarmGeneration >= 0 && alarmGeneration != sessionGeneration) {
            AppLog.d(TAG, "Ignoring stale in-memory session-complete alarm");
            return;
        }
        if (!isRunning) {
            return;
        }
        long remaining = timerEndElapsedRealtime - SystemClock.elapsedRealtime();
        SettleDecision decision = TimerSessionPolicy.decide(
                sessionType == 1 ? SessionPhase.RUNNING_BREAK : SessionPhase.RUNNING_STUDY,
                remaining,
                0L,
                PAUSE_TIMEOUT,
                alarmGeneration,
                sessionGeneration);
        if (decision == SettleDecision.COMPLETE_RUNNING || remaining <= 0L) {
            AppLog.d(TAG, "Session complete via AlarmManager");
            timeLeftInMillis = 0L;
            onTimerComplete();
        } else if (decision == SettleDecision.NONE) {
            TimerAlarmScheduler.scheduleSessionComplete(this, timerEndElapsedRealtime, sessionGeneration);
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
    }

    private void pauseSessionInternal(boolean recordReason, String reason) {
        stopTimerLoop();
        timeLeftInMillis = Math.max(0L, timerEndElapsedRealtime - SystemClock.elapsedRealtime());
        isRunning = false;
        isPaused = true;
        sessionGeneration++;
        pauseStartElapsedRealtime = SystemClock.elapsedRealtime();

        if (recordReason && sessionType == 0 && reason != null && !reason.isEmpty()) {
            sessionPauseReasons.add(reason);
        }
        pauseCount++;

        if (sessionType == 0) {
            stopTimerBlocking();
        }

        startPauseTimeoutCheck();
        startForegroundWithNotification();
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
                SystemClock.elapsedRealtime() + pauseTimeoutRemaining,
                sessionGeneration
        );
    }

    private void cancelPauseTimeoutCheck() {
        if (pauseTimeoutHandler != null && pauseTimeoutRunnable != null) {
            pauseTimeoutHandler.removeCallbacks(pauseTimeoutRunnable);
        }
        pauseTimeoutRunnable = null;
        TimerAlarmScheduler.cancelPauseTimeout(this);
    }

    /** 仅移除暂停超时 Handler，保留 Alarm（onDestroy 用）。 */
    private void stopPauseTimeoutHandlerOnly() {
        if (pauseTimeoutHandler != null && pauseTimeoutRunnable != null) {
            pauseTimeoutHandler.removeCallbacks(pauseTimeoutRunnable);
        }
    }

    private void handlePauseTimeoutAlarm(int alarmGeneration) {
        if (!restoredFromCheckpoint && ActiveSessionStore.hasActiveSession(this)) {
            ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(this);
            if (cp != null && alarmGeneration >= 0 && alarmGeneration != cp.generation) {
                AppLog.d(TAG, "Ignoring stale pause-timeout alarm");
                return;
            }
            restoreFromCheckpoint();
        } else if (alarmGeneration >= 0 && alarmGeneration != sessionGeneration) {
            return;
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
        if (!isCompletingSession.compareAndSet(false, true)) {
            return;
        }
        try {
            cancelPauseTimeoutCheck();
            stopTimerLoop();
            isRunning = false;
            isPaused = false;
            timeLeftInMillis = 0L;
            long settledId = sessionStartTime;

            if (sessionType == 0) {
                long duration = System.currentTimeMillis() - sessionStartTime;
                String userId = getSessionUserIdOrActive();
                List<String> reasons = new ArrayList<>(sessionPauseReasons);
                String pauseReasonTimeout = getString(R.string.timer_pause_reason_timeout);
                reasons.add(pauseReasonTimeout);
                statisticsRepository.recordFailedSession(
                        userId, sessionStartTime, duration, pauseReasonTimeout, currentTaskId, currentCategory,
                        Math.max(pauseCount, 1), reasons, null);
                stopTimerBlocking();
                FocusDndHelper.restoreDnd(this);
            }

            ActiveSessionStore.markSettledResult(
                    this, settledId, ActiveSessionStore.RESULT_FAILED_TIMEOUT);
            notifyTimerFinish();

            dispatchSessionAlert(SessionAlert.SESSION_FAILED);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        } finally {
            isCompletingSession.set(false);
        }
    }

    private void endSessionDueToLeave() {
        cancelPauseTimeoutCheck();
        stopTimerLoop();
        isRunning = false;
        isPaused = false;
        timeLeftInMillis = 0L;
        clearCheckpoint();

        if (sessionType == 0) {
            discardCurrentSessionBlockRecords();
            stopTimerBlocking();
            FocusDndHelper.restoreDnd(this);
        }

        notifyTimerFinish();

        dispatchSessionAlert(SessionAlert.SESSION_FAILED);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    public void resetTimer() {
        resetTimerInternal(true, true);
    }

    private void resetTimerForAccountSwitch() {
        resetTimerInternal(true, true);
    }

    private void resetTimerInternal(boolean notifyListeners, boolean stopServiceWhenDone) {
        cancelPauseTimeoutCheck();
        stopTimerLoop();
        boolean wasStudy = sessionType == 0;
        isRunning = false;
        isPaused = false;
        sessionUserId = "";
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
            discardCurrentSessionBlockRecords();
            stopTimerBlocking();
            FocusDndHelper.restoreDnd(this);
        }

        if (notifyListeners) {
            notifyTimerTick(timeLeftInMillis);
            notifyTimerReset();
            notifyStateChanged();
        } else {
            publishState();
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        if (stopServiceWhenDone) {
            stopSelf();
        }
    }

    /**
     * 使用 specialUse 前台服务类型，支持长时间番茄钟，避免 shortService 3 分钟超时 ANR。
     */
    private void startForegroundWithNotification() {
        enterForeground(createNotification());
    }

    private void enterForeground(Notification notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                            NOTIFICATION_ID_ONGOING,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    );
                } else {
                    startForeground(NOTIFICATION_ID_ONGOING, notification);
                }
            } else {
                startForeground(NOTIFICATION_ID_ONGOING, notification);
            }
        } catch (RuntimeException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isForegroundStartNotAllowed(e)) {
                AppLog.w(TAG, "Unable to promote timer service to foreground while app is in background", e);
                return;
            }
            throw e;
        }
    }

    private static boolean isForegroundStartNotAllowed(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof android.app.ForegroundServiceStartNotAllowedException) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    /** 服务已启动但无活跃计时时的占位通知（满足 startForeground 时限）。 */
    private Notification createIdleNotification() {
        return buildBaseNotificationBuilder()
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.timer_notification_ready))
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
        if (!isRunning && !isPaused && !awaitingPostBreakChoice) {
            return;
        }
        ActiveSessionStore.save(
                this,
                getSessionUserIdOrActive(),
                sessionStartTime,
                sessionGeneration,
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
            Uri uri = resolveRingtoneUri();
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

    @Nullable
    private Uri resolveRingtoneUri() {
        String ringtoneUri = settingsManager.getRingtoneUri();
        if ("silent".equals(ringtoneUri)) {
            return null;
        }
        if ("default".equals(ringtoneUri)) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return Uri.parse(ringtoneUri);
    }

    private Context getAudioAttributionContext() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return createAttributionContext("timerAlarm");
        }
        return this;
    }

    /**
     * 阶段切换提醒的统一入口：同步渠道铃声 → 发送提醒通知 → MediaPlayer/振动兜底。
     *
     * @param autoStartContext 仅用于 STUDY_STARTED / BREAK_ENDED：
     *                         休息结束后自动开始下一轮时为 true，文案与通知 ID 与手动路径区分。
     */
    private void dispatchSessionAlert(SessionAlert alert) {
        dispatchSessionAlert(alert, false);
    }

    private void dispatchSessionAlert(SessionAlert alert, boolean autoStartContext) {
        syncAlertChannelSound();

        boolean silent = "silent".equals(settingsManager.getRingtoneUri());
        NotificationCompat.Builder builder = buildSessionAlertNotification(alert, autoStartContext);
        applyAlertPolicy(builder, silent);

        boolean hasPermission = NotificationPermission.hasNotificationPermission(this);
        boolean inForeground = isAppInForeground();
        boolean useMediaPlayerFallback = !silent && (!hasPermission || inForeground);

        if (useMediaPlayerFallback) {
            builder.setSilent(true);
        }

        int notificationId = resolveAlertNotificationId(alert, autoStartContext);
        if (hasPermission) {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build());
        }

        if (silent) {
            return;
        }

        if (useMediaPlayerFallback) {
            playAlarm();
            VibrationHelper.vibrateAlert(this);
        }
    }

    /** 将用户设置的铃声同步到提醒通知渠道（Android 8+ 由渠道控制声音）。 */
    private void syncAlertChannelSound() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        String ringtonePref = settingsManager.getRingtoneUri();
        if (ringtonePref.equals(syncedAlertRingtoneUri)) {
            return;
        }

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID_ALERTS);
        if (channel == null) {
            createNotificationChannel();
            channel = manager.getNotificationChannel(CHANNEL_ID_ALERTS);
            if (channel == null) {
                return;
            }
        }

        if ("silent".equals(ringtonePref)) {
            channel.setSound(null, null);
            channel.enableVibration(false);
        } else {
            Uri uri = resolveRingtoneUri();
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(uri, attrs);
            channel.enableVibration(true);
        }
        manager.createNotificationChannel(channel);
        syncedAlertRingtoneUri = ringtonePref;
    }

    private void applyAlertPolicy(NotificationCompat.Builder builder, boolean silent) {
        builder.setOnlyAlertOnce(false);
        if (silent) {
            builder.setSilent(true);
        }
    }

    private int resolveAlertNotificationId(SessionAlert alert, boolean autoStartContext) {
        switch (alert) {
            case BREAK_STARTED:
                return NOTIFICATION_ID_ALERT_BREAK_STARTED;
            case STUDY_STARTED:
                return autoStartContext
                        ? NOTIFICATION_ID_ALERT_STUDY_AUTO_STARTED
                        : NOTIFICATION_ID_ALERT_STUDY_STARTED;
            case BREAK_ENDED:
                return NOTIFICATION_ID_ALERT_BREAK_ENDED;
            case SESSION_FAILED:
            default:
                return NOTIFICATION_ID_ALERT_FAILED;
        }
    }

    private NotificationCompat.Builder buildSessionAlertNotification(
            SessionAlert alert, boolean autoStartContext) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
                .setSmallIcon(R.drawable.ic_timer)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        switch (alert) {
            case STUDY_STARTED:
                builder.setContentTitle(autoStartContext
                        ? getString(R.string.timer_notification_study_auto_started_title)
                        : getString(R.string.timer_notification_study_started_title));
                builder.setContentText(autoStartContext
                        ? getString(R.string.timer_notification_break_end_auto)
                        : getString(R.string.timer_notification_study_started_message));
                builder.setCategory(NotificationCompat.CATEGORY_ALARM);
                builder.setContentIntent(buildTimerActivityPendingIntent(false));
                break;
            case BREAK_STARTED:
                builder.setContentTitle(isLongBreak
                        ? getString(R.string.timer_notification_study_complete_long_break_title)
                        : getString(R.string.timer_notification_study_complete_break_title));
                builder.setContentText(isLongBreak
                        ? getString(R.string.timer_notification_long_break_text)
                        : getString(R.string.timer_notification_short_break_text));
                builder.setCategory(NotificationCompat.CATEGORY_ALARM);
                builder.setContentIntent(buildTimerActivityPendingIntent(false));
                break;
            case BREAK_ENDED:
                builder.setContentTitle(getString(R.string.timer_notification_break_end_title));
                builder.setContentText(autoStartContext
                        ? getString(R.string.timer_notification_break_end_auto_lead)
                        : getString(R.string.timer_notification_break_end_message));
                builder.setCategory(NotificationCompat.CATEGORY_REMINDER);
                builder.setContentIntent(buildTimerActivityPendingIntent(true));
                if (!autoStartContext) {
                    builder.addAction(
                            R.drawable.ic_study,
                            getString(R.string.timer_notification_action_start_next),
                            buildServiceActionPendingIntent(ACTION_START, 12));
                    builder.addAction(
                            R.drawable.ic_stop,
                            getString(R.string.timer_end_session),
                            buildServiceActionPendingIntent(ACTION_RESET, 13));
                }
                break;
            case SESSION_FAILED:
                builder.setContentTitle(getString(R.string.timer_notification_fail_title));
                builder.setContentText(getString(R.string.timer_notification_fail_message));
                builder.setCategory(NotificationCompat.CATEGORY_REMINDER);
                break;
            default:
                break;
        }
        return builder;
    }

    private PendingIntent buildTimerActivityPendingIntent(boolean postBreak) {
        Intent openTimer = new Intent(this, TimerActivity.class);
        openTimer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (postBreak) {
            openTimer.putExtra(EXTRA_POST_BREAK, true);
        }
        int openFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            openFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(this, postBreak ? 10 : 11, openTimer, openFlags);
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        if (processes == null) {
            return false;
        }
        String packageName = getPackageName();
        for (ActivityManager.RunningAppProcessInfo info : processes) {
            if (packageName.equals(info.processName)
                    && info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
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
            syncedAlertRingtoneUri = "";
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
        if (isRunning || isPaused) {
            // 运行/暂停均通过 startForeground 刷新，保持轻量 FGS
            startForegroundWithNotification();
            return;
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            manager.notify(NOTIFICATION_ID_ONGOING, notification);
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

    private void cancelAllAlertNotifications(NotificationManagerCompat manager) {
        manager.cancel(NOTIFICATION_ID_ALERT_BREAK_STARTED);
        manager.cancel(NOTIFICATION_ID_ALERT_STUDY_STARTED);
        manager.cancel(NOTIFICATION_ID_ALERT_BREAK_ENDED);
        manager.cancel(NOTIFICATION_ID_ALERT_STUDY_AUTO_STARTED);
        manager.cancel(NOTIFICATION_ID_ALERT_FAILED);
        manager.cancel(LockScreenPresenter.NOTIFICATION_ID_LOCK_PRESENT);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // 用户划掉最近任务时落盘，便于下次启动恢复
        saveCheckpoint();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        unregisterLockScreenReceiver();
        if (lockScreenPresenter != null) {
            lockScreenPresenter.teardown();
        }
        boolean sessionActive = isRunning || isPaused || awaitingPostBreakChoice;
        if (sessionActive) {
            saveCheckpoint();
            // 只停 Handler，保留 Alarm 作为进程死后兜底
            stopHandlerTicksOnly();
            stopPauseTimeoutHandlerOnly();
        } else {
            cancelPauseTimeoutCheck();
            stopTimerLoop();
        }
        listeners.clear();

        if (!isRunning && !isPaused) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            manager.cancel(NOTIFICATION_ID_ONGOING);
            cancelAllAlertNotifications(manager);
        }
        super.onDestroy();
    }

    /** 普通计时页进入前台（抑制锁屏页强拉回）。 */
    public void notifyTimerUiResumed() {
        if (lockScreenPresenter != null) {
            lockScreenPresenter.onNormalTimerUiResumed();
        }
    }

    /**
     * 普通计时页离开前台。
     * @param dueToConfigChange 配置变更（旋转等）不触发强拉回
     */
    public void notifyTimerUiStopped(boolean dueToConfigChange) {
        if (lockScreenPresenter != null) {
            lockScreenPresenter.onNormalTimerUiStopped(dueToConfigChange);
        }
    }

    /** 锁屏专用计时页进入前台。 */
    public void notifyLockScreenUiResumed() {
        if (lockScreenPresenter != null) {
            lockScreenPresenter.onLockScreenUiResumed();
        }
    }

    /** 锁屏专用计时页离开前台。 */
    public void notifyLockScreenUiStopped(boolean dueToConfigChange) {
        if (lockScreenPresenter != null) {
            lockScreenPresenter.onLockScreenUiStopped(dueToConfigChange);
        }
    }

    /** 用户在锁屏计时页长按解锁，抑制立即再次强拉回。 */
    public void notifyUserRequestedUnlock() {
        if (lockScreenPresenter != null) {
            lockScreenPresenter.onUserRequestedUnlock();
        }
    }

    public boolean isLockScreenFullscreenEnabled() {
        return timerSettingsRepository != null
                && timerSettingsRepository.isLockScreenFullscreenEnabled();
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

    public void publishStateForSync() {
        publishState();
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

    /**
     * 学习阶段挂上计时联动屏蔽源，并把 sessionStartTime 写入拦截服务以便记账。
     * 「番茄内自动屏蔽」开启时会主动启动屏蔽；「我的」独立屏蔽已在跑时仅挂会话键，
     * 避免拦了应用却因缺少 SOURCE_TIMER 而不落库。
     */
    private void maybeStartTimerBlocking() {
        if (sessionType != 0 || isPaused || !isRunning) {
            return;
        }
        if (sessionStartTime <= 0L) {
            return;
        }
        boolean autoBlock = timerSettingsRepository.isAutoBlockDuringPomodoroEnabled();
        boolean blockingAlreadyRunning = AppBlockingServiceUtils.isServiceRunning(this);
        boolean standaloneEnabled = AppContainer.getInstance(this)
                .getUserAppBlockingRepository()
                .isEnabledForCurrentUser();
        if (!autoBlock && !blockingAlreadyRunning && !standaloneEnabled) {
            return;
        }
        if (!PermissionUtils.hasAllAppBlockingPermissions(this)) {
            return;
        }
        AppBlockingServiceUtils.startTimerBlocking(this, sessionStartTime);
    }

    private void stopTimerBlocking() {
        AppBlockingServiceUtils.stopTimerBlocking(this);
    }

    private void discardCurrentSessionBlockRecords() {
        if (sessionStartTime <= 0L) {
            return;
        }
        sessionBlockRecordRepository.deleteSessionRecords(
                getSessionUserIdOrActive(), sessionStartTime);
    }

    private void incrementTaskCompletedPomodoros(int taskId, String ownerUserId) {
        final int subTaskId = currentSubTaskId;
        AppExecutors.getInstance().diskIo(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            if (subTaskId >= 0) {
                SubTask subTask = db.subTaskDao().getSubTaskByIdSync(subTaskId);
                TodoItem parent = subTask != null
                        ? db.todoDao().getTodoByIdSync(subTask.parentTaskId)
                        : null;
                if (parent != null && ownerUserId.equals(parent.userId)) {
                    db.subTaskDao().incrementCompletedPomodoros(subTaskId);
                }
            } else {
                TodoItem task = db.todoDao().getTodoByIdSync(taskId);
                if (task != null && ownerUserId.equals(task.userId) && task.isSimple()) {
                    db.todoDao().incrementCompletedPomodoros(taskId);
                }
            }
        }, throwable -> AppLog.e(TAG, "Failed to increment completed pomodoros", throwable));
    }
}
