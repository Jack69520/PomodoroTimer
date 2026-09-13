package com.skyinit.pomodorotimer.ui.home.recovery;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.data.repository.TimerStateRepository;
import com.skyinit.pomodorotimer.domain.timer.RecoveryDecision;
import com.skyinit.pomodorotimer.domain.timer.SessionRecoveryPolicy;
import com.skyinit.pomodorotimer.domain.timer.TimerSessionPolicy;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;

/**
 * 启动时会话检测与恢复（MVI）。
 * <p>
 * 未到期：等 Service 真正进入活跃态后再强制打开计时页；已到期：展示结果弹窗。
 */
public class SessionRecoveryViewModel extends AndroidViewModel {

    private static final long RESTORE_TIMEOUT_MS = 3_000L;
    private static final long SETTLE_POLL_INTERVAL_MS = 200L;
    private static final int SETTLE_POLL_MAX = 15;

    private final MutableLiveData<SessionRecoveryUiState> uiState =
            new MutableLiveData<>(SessionRecoveryUiState.idle());
    private final MutableLiveData<SessionRecoveryEffect> effect = new MutableLiveData<>();

    private final TimerStateRepository timerStateRepository;
    private final AccountManager accountManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean terminalReached;
    private boolean evaluationInFlight;
    private boolean awaitingService;
    private boolean hasForcedOpened;
    private boolean controllerBound;
    private long forcedOpenSessionId;
    private int settleRetryCount;
    @Nullable
    private Observer<TimerUiState> restoreObserver;
    @Nullable
    private Runnable restoreTimeoutRunnable;
    @Nullable
    private Runnable settlePollRunnable;

    public SessionRecoveryViewModel(@NonNull Application application) {
        super(application);
        AppContainer container = ((App) application).getContainer();
        this.timerStateRepository = container.getTimerStateRepository();
        this.accountManager = AccountManager.getInstance(application);
    }

    public SessionRecoveryViewModel(@NonNull Application application,
                                    @NonNull TimerStateRepository timerStateRepository,
                                    @NonNull AccountManager accountManager) {
        super(application);
        this.timerStateRepository = timerStateRepository;
        this.accountManager = accountManager;
    }

    public LiveData<SessionRecoveryUiState> getUiState() {
        return uiState;
    }

    public LiveData<SessionRecoveryEffect> getEffect() {
        return effect;
    }

    /** Controller 消费副作用后调用，避免配置变更重复触发。 */
    public void consumeEffect() {
        effect.setValue(null);
    }

    public boolean markControllerBound() {
        if (controllerBound) {
            return false;
        }
        controllerBound = true;
        return true;
    }

    public boolean isBlockingShortcuts() {
        SessionRecoveryUiState state = uiState.getValue();
        return state != null && state.blocking;
    }

    public void dispatch(SessionRecoveryIntent intent) {
        if (intent == null) {
            return;
        }
        switch (intent) {
            case EVALUATE_STARTUP:
                evaluate(false);
                break;
            case RETRY_EVALUATE:
            case SERVICE_READY:
                if (awaitingService || !terminalReached) {
                    evaluate(true);
                }
                break;
            case OPEN_TIMER:
            case START_NEW_ROUND:
                emitEffect(SessionRecoveryEffect.FORCE_OPEN_TIMER);
                finishToIdle();
                break;
            case DISMISS:
            case ACK_DISCARD:
                finishToIdle();
                break;
            default:
                break;
        }
    }

    private void evaluate(boolean fromRetry) {
        if (terminalReached && !fromRetry) {
            return;
        }
        if (evaluationInFlight && !fromRetry) {
            return;
        }
        SessionRecoveryUiState current = uiState.getValue();
        if (current != null && current.phase == SessionRecoveryUiState.Phase.RESULT && !fromRetry) {
            return;
        }

        clearPendingWork();
        evaluationInFlight = true;
        awaitingService = false;
        setState(new SessionRecoveryUiState(
                SessionRecoveryUiState.Phase.EVALUATING,
                RecoveryDecision.ResultKind.NONE,
                0L,
                true));

        Application app = getApplication();
        String activeUserId = accountManager.getCurrentUserId();
        String lastResult = ActiveSessionStore.peekLastResult(app);
        long lastResultSessionId = ActiveSessionStore.peekLastResultSessionId(app);
        ActiveSessionStore.Checkpoint cp = ActiveSessionStore.load(app);
        boolean hasActive = cp != null;

        long remaining = 0L;
        long pauseElapsed = 0L;
        boolean running = false;
        boolean paused = false;
        boolean awaiting = false;
        String cpUserId = "";
        long sessionId = 0L;
        if (cp != null) {
            running = cp.running;
            paused = cp.paused;
            awaiting = cp.awaitingPostBreakChoice;
            remaining = ActiveSessionStore.computeRemainingMillis(cp);
            pauseElapsed = ActiveSessionStore.computePauseElapsedMillis(cp);
            cpUserId = cp.userId;
            sessionId = cp.sessionId > 0L ? cp.sessionId : cp.sessionStartTime;
        }

        RecoveryDecision decision = SessionRecoveryPolicy.decide(
                activeUserId,
                lastResult,
                lastResultSessionId,
                hasActive,
                cpUserId,
                running,
                paused,
                awaiting,
                remaining,
                pauseElapsed,
                TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS,
                sessionId);

        switch (decision.kind) {
            case DISCARD_FOREIGN_OR_GUEST:
                ActiveSessionStore.clear(app);
                ActiveSessionStore.consumeLastResult(app);
                evaluationInFlight = false;
                setState(new SessionRecoveryUiState(
                        SessionRecoveryUiState.Phase.DISCARDED,
                        RecoveryDecision.ResultKind.NONE,
                        0L,
                        false));
                emitEffect(SessionRecoveryEffect.SHOW_DISCARD_TOAST);
                terminalReached = true;
                break;
            case SHOW_RESULT:
                ActiveSessionStore.consumeLastResult(app);
                if (ActiveSessionStore.hasActiveSession(app)) {
                    TimerServiceLauncher.ensureRunning(app);
                    TimerServiceLauncher.deliverAction(app, TimerService.ACTION_EVALUATE_CHECKPOINT);
                }
                evaluationInFlight = false;
                setState(new SessionRecoveryUiState(
                        SessionRecoveryUiState.Phase.RESULT,
                        decision.resultKind,
                        decision.sessionId,
                        true));
                emitEffect(SessionRecoveryEffect.SHOW_RESULT_DIALOG);
                terminalReached = true;
                break;
            case CONTINUE_ACTIVE:
                beginContinueRestore(decision.sessionId);
                break;
            case SETTLE_THEN_RESULT:
                beginSettleThenResult(decision.sessionId);
                break;
            case NONE:
            default:
                evaluationInFlight = false;
                terminalReached = true;
                setState(SessionRecoveryUiState.idle());
                emitEffect(SessionRecoveryEffect.SHOW_EXACT_ALARM_HINT);
                break;
        }
    }

    private void beginContinueRestore(long sessionId) {
        setState(new SessionRecoveryUiState(
                SessionRecoveryUiState.Phase.AWAITING_SERVICE,
                RecoveryDecision.ResultKind.NONE,
                sessionId,
                true));
        awaitingService = true;

        TimerUiState already = timerStateRepository.getCurrentState();
        if (isActiveTimerState(already)) {
            onRestoreSucceeded(sessionId);
            return;
        }

        TimerServiceLauncher.ensureRunning(getApplication());
        TimerServiceLauncher.deliverAction(getApplication(), TimerService.ACTION_EVALUATE_CHECKPOINT);

        restoreObserver = state -> {
            if (isActiveTimerState(state)) {
                onRestoreSucceeded(sessionId);
            }
        };
        timerStateRepository.getState().observeForever(restoreObserver);

        restoreTimeoutRunnable = () -> {
            if (terminalReached) {
                return;
            }
            TimerUiState state = timerStateRepository.getCurrentState();
            if (isActiveTimerState(state)) {
                onRestoreSucceeded(sessionId);
                return;
            }
            // 仍有快照则保持等待 Service；否则失败
            if (ActiveSessionStore.hasActiveSession(getApplication())) {
                awaitingService = true;
                setState(new SessionRecoveryUiState(
                        SessionRecoveryUiState.Phase.AWAITING_SERVICE,
                        RecoveryDecision.ResultKind.NONE,
                        sessionId,
                        true));
                evaluationInFlight = false;
                return;
            }
            failRestore();
        };
        mainHandler.postDelayed(restoreTimeoutRunnable, RESTORE_TIMEOUT_MS);
        evaluationInFlight = false;
    }

    private void beginSettleThenResult(long sessionId) {
        setState(new SessionRecoveryUiState(
                SessionRecoveryUiState.Phase.AWAITING_SERVICE,
                RecoveryDecision.ResultKind.NONE,
                sessionId,
                true));
        awaitingService = true;
        settleRetryCount = 0;

        TimerServiceLauncher.ensureRunning(getApplication());
        TimerServiceLauncher.deliverAction(getApplication(), TimerService.ACTION_EVALUATE_CHECKPOINT);

        settlePollRunnable = new Runnable() {
            @Override
            public void run() {
                if (terminalReached) {
                    return;
                }
                Application app = getApplication();
                String result = ActiveSessionStore.peekLastResult(app);
                if (result != null && !result.isEmpty()) {
                    long resultId = ActiveSessionStore.peekLastResultSessionId(app);
                    ActiveSessionStore.consumeLastResult(app);
                    RecoveryDecision.ResultKind kind = mapResult(result);
                    evaluationInFlight = false;
                    awaitingService = false;
                    setState(new SessionRecoveryUiState(
                            SessionRecoveryUiState.Phase.RESULT,
                            kind,
                            resultId,
                            true));
                    emitEffect(SessionRecoveryEffect.SHOW_RESULT_DIALOG);
                    terminalReached = true;
                    return;
                }
                TimerUiState state = timerStateRepository.getCurrentState();
                if (isActiveTimerState(state)) {
                    // 结算后进入休息运行：按 CONTINUE 强制进页
                    onRestoreSucceeded(sessionId);
                    return;
                }
                settleRetryCount++;
                if (settleRetryCount >= SETTLE_POLL_MAX) {
                    if (!ActiveSessionStore.hasActiveSession(app)
                            && (ActiveSessionStore.peekLastResult(app) == null
                            || ActiveSessionStore.peekLastResult(app).isEmpty())) {
                        failRestore();
                        return;
                    }
                    awaitingService = true;
                    evaluationInFlight = false;
                    return;
                }
                mainHandler.postDelayed(this, SETTLE_POLL_INTERVAL_MS);
            }
        };
        mainHandler.postDelayed(settlePollRunnable, SETTLE_POLL_INTERVAL_MS);
        evaluationInFlight = false;
    }

    private void onRestoreSucceeded(long sessionId) {
        clearPendingWork();
        evaluationInFlight = false;
        awaitingService = false;
        setState(new SessionRecoveryUiState(
                SessionRecoveryUiState.Phase.CONTINUE_READY,
                RecoveryDecision.ResultKind.NONE,
                sessionId,
                true));
        if (!hasForcedOpened || forcedOpenSessionId != sessionId) {
            hasForcedOpened = true;
            forcedOpenSessionId = sessionId;
            emitEffect(SessionRecoveryEffect.FORCE_OPEN_TIMER);
        }
        terminalReached = true;
        // 打开页面后解除 blocking，允许后续交互
        setState(new SessionRecoveryUiState(
                SessionRecoveryUiState.Phase.CONTINUE_READY,
                RecoveryDecision.ResultKind.NONE,
                sessionId,
                false));
    }

    private void failRestore() {
        clearPendingWork();
        evaluationInFlight = false;
        awaitingService = false;
        terminalReached = true;
        setState(new SessionRecoveryUiState(
                SessionRecoveryUiState.Phase.FAILED,
                RecoveryDecision.ResultKind.NONE,
                0L,
                false));
        emitEffect(SessionRecoveryEffect.SHOW_RESTORE_FAILED_TOAST);
    }

    private void finishToIdle() {
        clearPendingWork();
        evaluationInFlight = false;
        awaitingService = false;
        terminalReached = true;
        setState(SessionRecoveryUiState.idle());
    }

    private void setState(SessionRecoveryUiState state) {
        uiState.setValue(state);
    }

    private void emitEffect(SessionRecoveryEffect value) {
        effect.setValue(value);
    }

    private void clearPendingWork() {
        if (restoreObserver != null) {
            timerStateRepository.getState().removeObserver(restoreObserver);
            restoreObserver = null;
        }
        if (restoreTimeoutRunnable != null) {
            mainHandler.removeCallbacks(restoreTimeoutRunnable);
            restoreTimeoutRunnable = null;
        }
        if (settlePollRunnable != null) {
            mainHandler.removeCallbacks(settlePollRunnable);
            settlePollRunnable = null;
        }
    }

    private static boolean isActiveTimerState(@Nullable TimerUiState state) {
        return state != null
                && (state.running || state.paused || state.awaitingPostBreakChoice);
    }

    private static RecoveryDecision.ResultKind mapResult(String result) {
        if (ActiveSessionStore.RESULT_BREAK_STARTED.equals(result)) {
            return RecoveryDecision.ResultKind.BREAK_STARTED;
        }
        if (ActiveSessionStore.RESULT_COMPLETED.equals(result)) {
            return RecoveryDecision.ResultKind.COMPLETED;
        }
        if (ActiveSessionStore.RESULT_FAILED_TIMEOUT.equals(result)) {
            return RecoveryDecision.ResultKind.FAILED_TIMEOUT;
        }
        return RecoveryDecision.ResultKind.NONE;
    }

    @Override
    protected void onCleared() {
        clearPendingWork();
        super.onCleared();
    }
}
