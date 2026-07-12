package com.skyinit.pomodorotimer.ui.account;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

/**
 * 注册/升级页 ViewModel。
 */
public class RegisterViewModel extends AndroidViewModel {

    private final UserSessionRepository sessionRepository;
    private final MutableLiveData<String> initialNickname = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> registerSuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<FormFieldError> fieldError = new SingleLiveEvent<>();

    public RegisterViewModel(@NonNull Application application,
                             UserSessionRepository sessionRepository) {
        super(application);
        this.sessionRepository = sessionRepository;
        if (sessionRepository.isLocalProfile()) {
            User user = sessionRepository.getCurrentUser();
            if (user != null) {
                initialNickname.setValue(user.nickname);
            }
        }
    }

    public LiveData<String> getInitialNickname() {
        return initialNickname;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Void> getRegisterSuccess() {
        return registerSuccess;
    }

    public LiveData<FormFieldError> getFieldError() {
        return fieldError;
    }

    public void register(String nickname, String password, String confirmPassword, String signature) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        String trimmedNickname = nickname == null ? "" : nickname.trim();
        String trimmedPassword = password == null ? "" : password.trim();
        String trimmedConfirm = confirmPassword == null ? "" : confirmPassword.trim();
        String trimmedSignature = signature == null ? "" : signature.trim();

        if (TextUtils.isEmpty(trimmedNickname)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_NICKNAME,
                    getApplication().getString(R.string.account_error_nickname_required)));
            return;
        }
        if (trimmedNickname.length() < 2 || trimmedNickname.length() > 20) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_NICKNAME,
                    getApplication().getString(R.string.account_error_nickname_length)));
            return;
        }
        if (TextUtils.isEmpty(trimmedPassword)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_PASSWORD,
                    getApplication().getString(R.string.account_error_password_required)));
            return;
        }
        if (TextUtils.isEmpty(trimmedConfirm)) {
            fieldError.setValue(new FormFieldError(FormFieldError.FIELD_CONFIRM_PASSWORD,
                    getApplication().getString(R.string.account_error_confirm_password_required)));
            return;
        }

        loading.setValue(true);
        sessionRepository.register(trimmedNickname, trimmedPassword, trimmedConfirm, null, trimmedSignature,
                new AccountManager.RegisterCallback() {
                    @Override
                    public void onSuccess(User user) {
                        loading.setValue(false);
                        toastMessage.setValue(getApplication().getString(
                                R.string.account_upgrade_success, user.userId));
                        registerSuccess.call();
                    }

                    @Override
                    public void onError(String message) {
                        loading.setValue(false);
                        toastMessage.setValue(message);
                    }
                });
    }
}
