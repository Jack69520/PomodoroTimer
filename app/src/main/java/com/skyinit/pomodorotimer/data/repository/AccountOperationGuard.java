package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.service.AppBlockingService;
import com.skyinit.pomodorotimer.service.TimerAlarmScheduler;
import com.skyinit.pomodorotimer.util.FocusDndHelper;

/**
 * 账户 ID 变化操作的统一守卫。
 * <p>
 * 该类只负责判断和清理跨账户风险状态，避免将服务状态判断散落在 Activity 或 AccountManager 中。
 */
public final class AccountOperationGuard {

    private final Context context;
    private final SettingsManager settingsManager;
    private final TimerStateRepository timerStateRepository;

    public AccountOperationGuard(Context context,
                                 SettingsManager settingsManager,
                                 TimerStateRepository timerStateRepository) {
        this.context = context.getApplicationContext();
        this.settingsManager = settingsManager;
        this.timerStateRepository = timerStateRepository;
    }

    public GuardState evaluate() {
        TimerUiState timerState = timerStateRepository.getCurrentState();
        boolean liveTimerActive = timerState != null
                && (timerState.running || timerState.paused || timerState.awaitingPostBreakChoice);
        boolean checkpointActive = ActiveSessionStore.hasActiveSession(context);
        boolean blockingEnabled = settingsManager.isAppBlockingEnabled();
        return new GuardState(liveTimerActive || checkpointActive, blockingEnabled);
    }

    /**
     * 用户确认继续账户操作后，先关闭屏蔽及系统副作用，确保新账户不会继承旧状态。
     */
    public void disableBlockingSideEffects() {
        settingsManager.setAppBlockingEnabled(false);
        Intent intent = new Intent(context, AppBlockingService.class);
        intent.putExtra("action", "stop_blocking");
        context.startService(intent);
        FocusDndHelper.restoreDnd(context);
    }

    /**
     * 注销账户等破坏性操作成功后兜底清理遗留计时外部状态。
     */
    public void clearTimerSideEffects() {
        ActiveSessionStore.clear(context);
        TimerAlarmScheduler.cancelAll(context);
        disableBlockingSideEffects();
    }

    public static final class GuardState {
        public final boolean timerActive;
        public final boolean blockingEnabled;

        private GuardState(boolean timerActive, boolean blockingEnabled) {
            this.timerActive = timerActive;
            this.blockingEnabled = blockingEnabled;
        }

        public boolean canContinueDirectly() {
            return !timerActive && !blockingEnabled;
        }
    }
}
