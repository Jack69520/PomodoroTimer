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
 * 登录页 ViewModel。
 */
public class LoginViewModel extends AndroidViewModel {

    private final UserSessionRepository sessionRepository;
    private final AccountOperationGuard accountOperationGuard;
    private PendingLogin pendingLogin;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> loginSuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<FormFieldError> fieldError = new SingleLiveEvent<>();
    private final SingleLiveEvent<AccountOperationGuard.GuardState> guardPrompt = new SingleLiveEvent<>();

    public LoginViewModel(@NonNull Application application,
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

    public LiveData<Void> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<FormFieldError> getFieldError() {
        return fieldError;
    }

    public LiveData<AccountOperationGuard.GuardState> getGuardPrompt() {
        return guardPrompt;
    }

    public void login(String userId, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        String trimmedId = userId == null ? "" : userId.trim();
        String trimmedPassword = password == null ? "" : password.trim();

        if (TextUtils.isEmpty(trimmedId)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_USER_ID,
                    getApplication().getString(R.string.account_error_user_id_required)));
            return;
        }
        if (trimmedId.length() != 10) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_USER_ID,
                    getApplication().getString(R.string.account_error_user_id_length)));
            return;
        }
        if (TextUtils.isEmpty(trimmedPassword)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_PASSWORD,
                    getApplication().getString(R.string.account_error_password_required)));
            return;
        }

        pendingLogin = new PendingLogin(trimmedId, trimmedPassword);
        AccountOperationGuard.GuardState guardState = accountOperationGuard.evaluate();
        if (guardState.timerActive) {
            toastMessage.setValue(getApplication().getString(R.string.account_guard_timer_active));
            return;
        }
        if (guardState.blockingEnabled) {
            guardPrompt.setValue(guardState);
            return;
        }
        executePendingLogin(false);
    }

    public void continueAfterDisablingBlocking() {
        if (pendingLogin == null) {
            return;
        }
        executePendingLogin(true);
    }

    private void executePendingLogin(boolean disableBlocking) {
        PendingLogin request = pendingLogin;
        if (request == null) {
            return;
        }
        if (disableBlocking) {
            accountOperationGuard.disableBlockingSideEffects();
        }
        loading.setValue(true);
        sessionRepository.login(request.userId, request.password, new AccountManager.LoginCallback() {
            @Override
            public void onSuccess(com.skyinit.pomodorotimer.data.entity.User user) {
                pendingLogin = null;
                loading.setValue(false);
                loginSuccess.call();
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                toastMessage.setValue(message);
            }
        });
    }

    private static final class PendingLogin {
        final String userId;
        final String password;

        PendingLogin(String userId, String password) {
            this.userId = userId;
            this.password = password;
        }
    }
}
