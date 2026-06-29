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
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

/**
 * 账户找回页 ViewModel。
 */
public class AccountRecoveryViewModel extends AndroidViewModel {

    private final UserSessionRepository sessionRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> recoverySuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<FormFieldError> fieldError = new SingleLiveEvent<>();

    public AccountRecoveryViewModel(@NonNull Application application,
                                    UserSessionRepository sessionRepository) {
        super(application);
        this.sessionRepository = sessionRepository;
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

        loading.setValue(true);
        sessionRepository.recoverLogin(trimmedId, trimmedNickname, new AccountManager.LoginCallback() {
            @Override
            public void onSuccess(com.skyinit.pomodorotimer.data.entity.User user) {
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
}
