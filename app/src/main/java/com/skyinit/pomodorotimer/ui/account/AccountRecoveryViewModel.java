package com.skyinit.pomodorotimer.ui.account;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.AccountOperationGuard;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

/**
 * 账户找回页 ViewModel。
 */
public class AccountRecoveryViewModel extends AndroidViewModel {

    private final UserSessionRepository sessionRepository;
    private final AccountOperationGuard accountOperationGuard;
    private PendingRecovery pendingRecovery;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> recoverySuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<FormFieldError> fieldError = new SingleLiveEvent<>();
    private final SingleLiveEvent<AccountOperationGuard.GuardState> guardPrompt = new SingleLiveEvent<>();

    public AccountRecoveryViewModel(@NonNull Application application,
                                    UserSessionRepository sessionRepository,
                                    AccountOperationGuard accountOperationGuard) {
        super(application);
        this.sessionRepository = sessionRepository;
        this.accountOperationGuard = accountOperationGuard;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Void> getRecoverySuccess() {
        return recoverySuccess;
    }

    public LiveData<FormFieldError> getFieldError() {
        return fieldError;
    }

    public LiveData<AccountOperationGuard.GuardState> getGuardPrompt() {
        return guardPrompt;
    }

    public void recover(String userId, String nickname) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        String trimmedId = userId == null ? "" : userId.trim();
        String trimmedNickname = nickname == null ? "" : nickname.trim();

        if (TextUtils.isEmpty(trimmedId) || trimmedId.length() != 10) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_USER_ID,
                    getApplication().getString(R.string.account_error_user_id_10_digits)));
            return;
        }
        if (TextUtils.isEmpty(trimmedNickname)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_NICKNAME,
                    getApplication().getString(R.string.account_error_account_name_required)));
            return;
        }

        pendingRecovery = new PendingRecovery(trimmedId, trimmedNickname);
        AccountOperationGuard.GuardState guardState = accountOperationGuard.evaluate();
        if (guardState.timerActive) {
            toastMessage.setValue(getApplication().getString(R.string.account_guard_timer_active));
            return;
        }
        if (guardState.blockingEnabled) {
            guardPrompt.setValue(guardState);
            return;
        }
        executePendingRecovery(false);
    }

    public void continueAfterDisablingBlocking() {
        executePendingRecovery(true);
    }

    private void executePendingRecovery(boolean disableBlocking) {
        PendingRecovery request = pendingRecovery;
        if (request == null) {
            return;
        }
        if (disableBlocking) {
            accountOperationGuard.disableBlockingSideEffects();
        }
        loading.setValue(true);
        sessionRepository.recoverLogin(request.userId, request.nickname, new AccountManager.LoginCallback() {
            @Override
            public void onSuccess(com.skyinit.pomodorotimer.data.entity.User user) {
                pendingRecovery = null;
                loading.setValue(false);
                toastMessage.setValue(getApplication().getString(
                        R.string.account_toast_recovery_verified));
                recoverySuccess.call();
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                toastMessage.setValue(message);
            }
        });
    }

    private static final class PendingRecovery {
        final String userId;
        final String nickname;

        PendingRecovery(String userId, String nickname) {
            this.userId = userId;
            this.nickname = nickname;
        }
    }
}
