package com.skyinit.pomodorotimer.ui.account;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.model.AccountUiState;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.AccountOperationGuard;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

/**
 * 账户详情页 ViewModel：可观察 UI 状态与账户操作。
 */
public class AccountViewModel extends AndroidViewModel {

    private final UserSessionRepository sessionRepository;
    private final AccountOperationGuard accountOperationGuard;
    private PendingAccountOperation pendingOperation;
    private final MutableLiveData<AccountUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> deleteSuccessDialog = new SingleLiveEvent<>();
    private final SingleLiveEvent<AccountOperationGuard.GuardState> guardPrompt = new SingleLiveEvent<>();
    private boolean actionInProgress;

    private final Observer<User> activeUserObserver = this::onActiveUserChanged;

    public AccountViewModel(@NonNull Application application,
                            UserSessionRepository sessionRepository,
                            AccountOperationGuard accountOperationGuard) {
        super(application);
        this.sessionRepository = sessionRepository;
        this.accountOperationGuard = accountOperationGuard;
        sessionRepository.getActiveUser().observeForever(activeUserObserver);
        User current = sessionRepository.getCurrentUser();
        if (current != null) {
            uiState.setValue(buildUiState(current, false));
        }
    }

    public LiveData<AccountUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Void> getDeleteSuccessDialog() {
        return deleteSuccessDialog;
    }

    public LiveData<AccountOperationGuard.GuardState> getGuardPrompt() {
        return guardPrompt;
    }

    public void logout() {
        if (actionInProgress) {
            return;
        }
        if (!sessionRepository.isRegistered()) {
            toastMessage.setValue(getApplication().getString(R.string.account_registered_only));
            return;
        }
        pendingOperation = PendingAccountOperation.LOGOUT;
        if (!ensureAccountOperationAllowed()) {
            return;
        }
        executePendingOperation(false);
    }

    public void deleteAccount() {
        if (actionInProgress) {
            return;
        }
        pendingOperation = PendingAccountOperation.DELETE_ACCOUNT;
        if (!ensureAccountOperationAllowed()) {
            return;
        }
        executePendingOperation(false);
    }

    public void updateProfile(String nickname, String avatarPath, String signature) {
        if (actionInProgress) {
            return;
        }
        setActionInProgress(true);
        sessionRepository.updateProfile(nickname, avatarPath, signature, new AccountManager.ProfileUpdateCallback() {
            @Override
            public void onSuccess() {
                setActionInProgress(false);
                toastMessage.setValue(getApplication().getString(R.string.account_toast_update_success));
            }

            @Override
            public void onError(String message) {
                setActionInProgress(false);
                toastMessage.setValue(message);
            }
        });
    }

    public void continueAfterDisablingBlocking() {
        executePendingOperation(true);
    }

    private boolean ensureAccountOperationAllowed() {
        AccountOperationGuard.GuardState guardState = accountOperationGuard.evaluate();
        if (guardState.timerActive) {
            toastMessage.setValue(getApplication().getString(R.string.account_guard_timer_active));
            return false;
        }
        if (guardState.blockingEnabled) {
            guardPrompt.setValue(guardState);
            return false;
        }
        return true;
    }

    private void executePendingOperation(boolean disableBlocking) {
        if (pendingOperation == null || actionInProgress) {
            return;
        }
        if (disableBlocking) {
            accountOperationGuard.disableBlockingSideEffects();
        }
        if (pendingOperation == PendingAccountOperation.LOGOUT) {
            executeLogout();
        } else if (pendingOperation == PendingAccountOperation.DELETE_ACCOUNT) {
            executeDeleteAccount();
        }
    }

    private void executeLogout() {
        setActionInProgress(true);
        sessionRepository.logout(new AccountManager.LogoutCallback() {
            @Override
            public void onSuccess() {
                pendingOperation = null;
                accountOperationGuard.clearTimerSideEffects();
                setActionInProgress(false);
                toastMessage.setValue(getApplication().getString(R.string.account_logout_success));
            }

            @Override
            public void onError(String message) {
                setActionInProgress(false);
                toastMessage.setValue(message);
            }
        });
    }

    private void executeDeleteAccount() {
        setActionInProgress(true);
        sessionRepository.deleteCurrentAccount(new AccountManager.AccountDeletionCallback() {
            @Override
            public void onSuccess() {
                pendingOperation = null;
                accountOperationGuard.clearTimerSideEffects();
                setActionInProgress(false);
                deleteSuccessDialog.call();
            }

            @Override
            public void onError(String message) {
                setActionInProgress(false);
                toastMessage.setValue(message);
            }
        });
    }

    private void onActiveUserChanged(User user) {
        if (user == null) {
            return;
        }
        uiState.setValue(buildUiState(user, actionInProgress));
    }

    private AccountUiState buildUiState(User user, boolean inProgress) {
        Application app = getApplication();
        String idLabel = user.isLocalProfile()
                ? app.getString(R.string.account_local_profile_label)
                : app.getString(R.string.account_id_format, user.userId);
        String signature = user.signature != null && !user.signature.isEmpty()
                ? user.signature
                : app.getString(R.string.account_default_signature);
        return new AccountUiState(
                user.userId,
                idLabel,
                user.nickname,
                signature,
                user.avatarPath,
                user.isRegistered(),
                inProgress
        );
    }

    private void setActionInProgress(boolean inProgress) {
        actionInProgress = inProgress;
        AccountUiState current = uiState.getValue();
        if (current != null) {
            uiState.setValue(current.withActionInProgress(inProgress));
        }
    }

    private enum PendingAccountOperation {
        LOGOUT,
        DELETE_ACCOUNT
    }

    @Override
    protected void onCleared() {
        sessionRepository.getActiveUser().removeObserver(activeUserObserver);
        super.onCleared();
    }
}
