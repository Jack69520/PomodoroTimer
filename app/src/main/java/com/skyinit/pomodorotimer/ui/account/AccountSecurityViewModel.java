package com.skyinit.pomodorotimer.ui.account;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.repository.AccountOperationGuard;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

/**
 * 「账户与安全」ViewModel（MVI）：修改密码 / 退出 / 切号 / 注销。
 */
public class AccountSecurityViewModel extends ViewModel
        implements AccountSessionActionCoordinator.Listener {

    private final UserSessionRepository sessionRepository;
    private final AccountSessionActionCoordinator sessionActions;

    private final MutableLiveData<AccountSecurityUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<AccountSecurityEffect> effects = new SingleLiveEvent<>();

    private boolean actionInProgress;
    private boolean registered;

    private final Observer<User> activeUserObserver = this::onActiveUserChanged;

    public AccountSecurityViewModel(@NonNull UserSessionRepository sessionRepository,
                                    @NonNull AccountOperationGuard accountOperationGuard) {
        this.sessionRepository = sessionRepository;
        this.sessionActions = new AccountSessionActionCoordinator(
                sessionRepository, accountOperationGuard, this);
        sessionRepository.getActiveUser().observeForever(activeUserObserver);
        onActiveUserChanged(sessionRepository.getCurrentUser());
    }

    @NonNull
    public LiveData<AccountSecurityUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<AccountSecurityEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull AccountSecurityIntent intent) {
        switch (intent.type) {
            case CHANGE_PASSWORD:
                openChangePassword();
                break;
            case LOGOUT:
                sessionActions.requestLogout();
                break;
            case CONFIRM_LOGOUT:
                sessionActions.confirmLogout();
                break;
            case SWITCH_ACCOUNT:
                sessionActions.requestSwitchAccount();
                break;
            case DELETE_ACCOUNT:
                sessionActions.requestDelete();
                break;
            case CONFIRM_DELETE:
                sessionActions.confirmDelete();
                break;
            case CONTINUE_AFTER_DISABLE_BLOCKING:
                sessionActions.continueAfterDisablingBlocking();
                break;
            case LOGIN:
                effects.setValue(AccountSecurityEffect.startActivity(LoginActivity.class));
                break;
            case UPGRADE_REGISTER:
                effects.setValue(AccountSecurityEffect.startActivity(RegisterActivity.class));
                break;
            default:
                break;
        }
    }

    private void openChangePassword() {
        if (actionInProgress) {
            return;
        }
        if (!registered) {
            effects.setValue(AccountSecurityEffect.showToast(R.string.account_registered_only));
            return;
        }
        effects.setValue(AccountSecurityEffect.startActivity(ChangePasswordActivity.class));
    }

    private void onActiveUserChanged(@Nullable User user) {
        boolean wasRegistered = registered;
        registered = user != null;
        if (wasRegistered != registered) {
            sessionActions.onSessionUserChanged();
            // 进行中的退出/注销由协调器回调收尾，勿强行清 busy
            if (!sessionActions.isBusy()) {
                actionInProgress = false;
            }
        }
        publishState();
    }

    private void publishState() {
        uiState.setValue(new AccountSecurityUiState(registered, actionInProgress));
    }

    @Override
    public void onBusyChanged(boolean busy) {
        actionInProgress = busy;
        publishState();
    }

    @Override
    public void onToast(int resId) {
        effects.setValue(AccountSecurityEffect.showToast(resId));
    }

    @Override
    public void onToastText(@NonNull String message) {
        if (!message.isEmpty()) {
            effects.setValue(AccountSecurityEffect.showToastText(message));
        }
    }

    @Override
    public void onGuardPrompt() {
        effects.setValue(AccountSecurityEffect.showGuardPrompt());
    }

    @Override
    public void onLogoutConfirm() {
        effects.setValue(AccountSecurityEffect.showLogoutConfirm());
    }

    @Override
    public void onDeleteConfirm() {
        effects.setValue(AccountSecurityEffect.showDeleteConfirm());
    }

    @Override
    public void onDeleteSuccess() {
        effects.setValue(AccountSecurityEffect.showDeleteSuccess());
    }

    @Override
    public void onNavigateToLogin() {
        effects.setValue(AccountSecurityEffect.startActivity(LoginActivity.class));
    }

    @Override
    public void onLogoutSuccess() {
        // SingleLiveEvent 不可连续投递：合并 Toast + 关闭
        effects.setValue(AccountSecurityEffect.finishWithToast(R.string.account_logout_success));
    }

    @Override
    protected void onCleared() {
        sessionActions.clear();
        sessionRepository.getActiveUser().removeObserver(activeUserObserver);
        super.onCleared();
    }
}
