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
 * 修改密码页 ViewModel。
 */
public class ChangePasswordViewModel extends AndroidViewModel {

    private final UserSessionRepository sessionRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> updateSuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<FormFieldError> fieldError = new SingleLiveEvent<>();

    public ChangePasswordViewModel(@NonNull Application application,
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

    public LiveData<Void> getUpdateSuccess() {
        return updateSuccess;
    }

    public LiveData<FormFieldError> getFieldError() {
        return fieldError;
    }

    public void updatePassword(String oldPassword, String newPassword, String confirmPassword) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        String oldPwd = oldPassword == null ? "" : oldPassword;
        String newPwd = newPassword == null ? "" : newPassword;
        String confirmPwd = confirmPassword == null ? "" : confirmPassword;

        if (TextUtils.isEmpty(oldPwd)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_OLD_PASSWORD,
                    getApplication().getString(R.string.account_error_old_password_required)));
            return;
        }
        if (TextUtils.isEmpty(newPwd)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_NEW_PASSWORD,
                    getApplication().getString(R.string.account_error_new_password_required)));
            return;
        }
        if (TextUtils.isEmpty(confirmPwd)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_CONFIRM_PASSWORD,
                    getApplication().getString(R.string.account_error_confirm_new_password_required)));
            return;
        }

        loading.setValue(true);
        sessionRepository.updatePassword(oldPwd, newPwd, confirmPwd, new AccountManager.PasswordUpdateCallback() {
            @Override
            public void onSuccess() {
                loading.setValue(false);
                toastMessage.setValue(getApplication().getString(R.string.account_toast_password_changed));
                updateSuccess.call();
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                Application app = getApplication();
                if (app.getString(R.string.account_error_wrong_old_password).equals(message)) {
                    fieldError.setValue(new FormFieldError(FormFieldError.FIELD_OLD_PASSWORD,
                            app.getString(R.string.account_error_old_password_wrong)));
                } else if (message.startsWith(
                        app.getString(R.string.account_error_new_password_invalid, ""))) {
                    fieldError.setValue(new FormFieldError(FormFieldError.FIELD_TIP, message));
                } else if (app.getString(R.string.account_error_password_mismatch).equals(message)) {
                    fieldError.setValue(new FormFieldError(
                            FormFieldError.FIELD_CONFIRM_PASSWORD,
                            app.getString(R.string.account_error_password_mismatch)));
                } else {
                    toastMessage.setValue(message);
                }
            }
        });
    }
}
