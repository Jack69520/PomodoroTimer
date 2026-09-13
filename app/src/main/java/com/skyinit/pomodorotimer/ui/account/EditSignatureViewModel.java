package com.skyinit.pomodorotimer.ui.account;

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 修改签名页 ViewModel。
 */
public class EditSignatureViewModel extends AndroidViewModel {

    static final int MAX_LEN = 60;

    private final UserSessionRepository sessionRepository;
    private final MutableLiveData<String> signature = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final SingleLiveEvent<Integer> toastRes = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> toastText = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> finishPage = new SingleLiveEvent<>();
    private final AtomicInteger updateGeneration = new AtomicInteger(0);

    private final Observer<User> activeUserObserver = user -> {
        if (user == null) {
            return;
        }
        Boolean busy = saving.getValue();
        if (busy != null && busy) {
            return;
        }
        signature.setValue(user.signature != null ? user.signature : "");
    };

    public EditSignatureViewModel(@NonNull Application application,
                                  UserSessionRepository sessionRepository) {
        super(application);
        this.sessionRepository = sessionRepository;
        sessionRepository.getActiveUser().observeForever(activeUserObserver);
        User current = sessionRepository.getCurrentUser();
        if (current != null) {
            signature.setValue(current.signature != null ? current.signature : "");
        }
    }

    public LiveData<String> getSignature() {
        return signature;
    }

    public LiveData<Boolean> getSaving() {
        return saving;
    }

    public LiveData<Integer> getToastRes() {
        return toastRes;
    }

    public LiveData<String> getToastText() {
        return toastText;
    }

    public LiveData<Void> getFinishPage() {
        return finishPage;
    }

    @MainThread
    public void save(@Nullable String raw) {
        Boolean busy = saving.getValue();
        if (busy != null && busy) {
            return;
        }
        String value = raw != null ? raw.trim() : "";
        if (value.length() > MAX_LEN) {
            toastRes.setValue(R.string.account_error_signature_length);
            return;
        }
        String current = signature.getValue();
        if (current == null) {
            current = "";
        }
        if (value.equals(current)) {
            finishPage.call();
            return;
        }
        final int generation = updateGeneration.incrementAndGet();
        saving.setValue(true);
        sessionRepository.updateProfile(null, null, value, new AccountManager.ProfileUpdateCallback() {
            @Override
            public void onSuccess() {
                if (generation != updateGeneration.get()) {
                    return;
                }
                saving.setValue(false);
                toastRes.setValue(R.string.account_toast_update_success);
                finishPage.call();
            }

            @Override
            public void onError(String message) {
                if (generation != updateGeneration.get()) {
                    return;
                }
                saving.setValue(false);
                toastText.setValue(message);
            }
        });
    }

    @Override
    protected void onCleared() {
        updateGeneration.incrementAndGet();
        sessionRepository.getActiveUser().removeObserver(activeUserObserver);
        super.onCleared();
    }
}
