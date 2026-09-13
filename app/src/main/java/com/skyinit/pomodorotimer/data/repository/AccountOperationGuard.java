package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.Intent;

import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.service.AppBlockingService;
import com.skyinit.pomodorotimer.service.TimerAlarmScheduler;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.util.FocusDndHelper;

/**
 * 账户切换操作的统一守卫：判断计时/屏蔽风险并清理副作用。
 * 切号时只停止屏蔽服务，不改写目标用户在 DB 中的 enabled 偏好。
 */
public final class AccountOperationGuard {

    private final Context context;
    private final UserAppBlockingRepository userAppBlockingRepository;
    private final TimerStateRepository timerStateRepository;

    public AccountOperationGuard(Context context,
                                 UserAppBlockingRepository userAppBlockingRepository,
                                 TimerStateRepository timerStateRepository) {
        this.context = context.getApplicationContext();
        this.userAppBlockingRepository = userAppBlockingRepository;
        this.timerStateRepository = timerStateRepository;
    }

    public GuardState evaluate() {
        TimerUiState timerState = timerStateRepository.getCurrentState();
        boolean liveTimerActive = timerState != null
                && (timerState.running || timerState.paused || timerState.awaitingPostBreakChoice);
        boolean checkpointActive = ActiveSessionStore.hasActiveSession(context);
        boolean blockingEnabled = userAppBlockingRepository.isEnabledForCurrentUser();
        return new GuardState(liveTimerActive || checkpointActive, blockingEnabled);
    }

    /** 停止屏蔽服务与 DND，不修改用户 enabled 持久化偏好。 */
    public void stopBlockingServiceSideEffects() {
        Intent intent = new Intent(context, AppBlockingService.class);
        intent.putExtra("action", "stop_blocking");
        context.startService(intent);
        FocusDndHelper.restoreDnd(context);
    }

    /** 用户确认关闭屏蔽后：持久化 false 并停服务。 */
    public void disableBlockingSideEffects() {
        userAppBlockingRepository.setEnabledForCurrentUser(false);
        stopBlockingServiceSideEffects();
    }

    public void clearTimerSideEffects() {
        ActiveSessionStore.clear(context);
        TimerAlarmScheduler.cancelAll(context);
        stopBlockingServiceSideEffects();
    }

    public void onActiveAccountSwitched() {
        clearTimerSideEffects();
        TimerServiceLauncher.deliverAction(context, TimerService.ACTION_ACCOUNT_SWITCH_RESET);
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
