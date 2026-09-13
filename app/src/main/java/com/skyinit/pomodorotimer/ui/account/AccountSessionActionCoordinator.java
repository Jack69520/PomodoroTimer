package com.skyinit.pomodorotimer.ui.account;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.AccountOperationGuard;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 账户会话安全操作协调器：退出 / 切号 / 注销的守卫、确认与执行。
 * <p>
 * 供「个人资料」与「账户与安全」共用，保证高内聚、防重复点击与异步竞态。
 * 须在主线程调用；异步回调用 generation 丢弃过期结果。
 */
public final class AccountSessionActionCoordinator {

    public enum Operation {
        LOGOUT,
        SWITCH_ACCOUNT,
        DELETE_ACCOUNT
    }

    /**
     * 副作用监听：由各页面 ViewModel 转发为 Effect。
     */
    public interface Listener {
        @MainThread
        void onBusyChanged(boolean busy);

        @MainThread
        void onToast(@StringRes int resId);

        @MainThread
        void onToastText(@NonNull String message);

        @MainThread
        void onGuardPrompt();

        @MainThread
        void onLogoutConfirm();

        @MainThread
        void onDeleteConfirm();

        @MainThread
        void onDeleteSuccess();

        @MainThread
        void onNavigateToLogin();

        @MainThread
        void onLogoutSuccess();
    }

    private final UserSessionRepository sessionRepository;
    private final AccountOperationGuard accountOperationGuard;
    private final Listener listener;

    private final AtomicInteger opGeneration = new AtomicInteger(0);
    private boolean busy;
    @Nullable
    private Operation pendingOperation;

    public AccountSessionActionCoordinator(@NonNull UserSessionRepository sessionRepository,
                                           @NonNull AccountOperationGuard accountOperationGuard,
                                           @NonNull Listener listener) {
        this.sessionRepository = sessionRepository;
        this.accountOperationGuard = accountOperationGuard;
        this.listener = listener;
    }

    public boolean isBusy() {
        return busy;
    }

    /** 用户切换：作废未开始的 pending；进行中的异步回调仍可完成。 */
    @MainThread
    public void onSessionUserChanged() {
        pendingOperation = null;
    }

    /** ViewModel 销毁：作废一切进行中的回调。 */
    @MainThread
    public void clear() {
        opGeneration.incrementAndGet();
        pendingOperation = null;
        busy = false;
    }

    @MainThread
    public void requestLogout() {
        if (busy) {
            return;
        }
        if (!sessionRepository.isRegistered()) {
            listener.onToast(R.string.account_registered_only);
            return;
        }
        listener.onLogoutConfirm();
    }

    @MainThread
    public void confirmLogout() {
        beginGuarded(Operation.LOGOUT);
    }

    @MainThread
    public void requestSwitchAccount() {
        beginGuarded(Operation.SWITCH_ACCOUNT);
    }

    @MainThread
    public void requestDelete() {
        if (busy) {
            return;
        }
        if (!sessionRepository.isRegistered()) {
            listener.onToast(R.string.account_registered_only);
            return;
        }
        listener.onDeleteConfirm();
    }

    @MainThread
    public void confirmDelete() {
        beginGuarded(Operation.DELETE_ACCOUNT);
    }

    @MainThread
    public void continueAfterDisablingBlocking() {
        executePending(true);
    }

    private void beginGuarded(@NonNull Operation operation) {
        if (busy) {
            return;
        }
        if (!sessionRepository.isRegistered()) {
            listener.onToast(R.string.account_registered_only);
            return;
        }
        pendingOperation = operation;
        if (!ensureAllowed()) {
            return;
        }
        executePending(false);
    }

    private boolean ensureAllowed() {
        AccountOperationGuard.GuardState guardState = accountOperationGuard.evaluate();
        if (guardState.timerActive) {
            pendingOperation = null;
            listener.onToast(R.string.account_guard_timer_active);
            return false;
        }
        if (guardState.blockingEnabled) {
            listener.onGuardPrompt();
            return false;
        }
        return true;
    }

    private void executePending(boolean disableBlocking) {
        if (pendingOperation == null || busy) {
            return;
        }
        if (disableBlocking) {
            accountOperationGuard.disableBlockingSideEffects();
        }
        Operation op = pendingOperation;
        if (op == Operation.SWITCH_ACCOUNT) {
            pendingOperation = null;
            listener.onNavigateToLogin();
            return;
        }
        if (op == Operation.LOGOUT) {
            executeLogout();
        } else if (op == Operation.DELETE_ACCOUNT) {
            executeDelete();
        }
    }

    private void executeLogout() {
        final int generation = opGeneration.incrementAndGet();
        setBusy(true);
        sessionRepository.logout(new AccountManager.LogoutCallback() {
            @Override
            public void onSuccess() {
                if (generation != opGeneration.get()) {
                    return;
                }
                pendingOperation = null;
                accountOperationGuard.clearTimerSideEffects();
                setBusy(false);
                listener.onLogoutSuccess();
            }

            @Override
            public void onError(String message) {
                if (generation != opGeneration.get()) {
                    return;
                }
                setBusy(false);
                listener.onToastText(message != null ? message : "");
            }
        });
    }

    private void executeDelete() {
        final int generation = opGeneration.incrementAndGet();
        setBusy(true);
        sessionRepository.deleteCurrentAccount(new AccountManager.AccountDeletionCallback() {
            @Override
            public void onSuccess() {
                if (generation != opGeneration.get()) {
                    return;
                }
                pendingOperation = null;
                accountOperationGuard.clearTimerSideEffects();
                setBusy(false);
                listener.onDeleteSuccess();
            }

            @Override
            public void onError(String message) {
                if (generation != opGeneration.get()) {
                    return;
                }
                setBusy(false);
                listener.onToastText(message != null ? message : "");
            }
        });
    }

    private void setBusy(boolean value) {
        if (busy == value) {
            return;
        }
        busy = value;
        listener.onBusyChanged(value);
    }
}
