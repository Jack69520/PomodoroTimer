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
 * 修改昵称页 ViewModel。
 */
public class EditNicknameViewModel extends AndroidViewModel {

    static final int MIN_LEN = 2;
    static final int MAX_LEN = 20;

    private final UserSessionRepository sessionRepository;
    private final MutableLiveData<String> nickname = new MutableLiveData<>("");
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
        nickname.setValue(user.nickname != null ? user.nickname : "");
    };

    public EditNicknameViewModel(@NonNull Application application,
                                 UserSessionRepository sessionRepository) {
        super(application);
        this.sessionRepository = sessionRepository;
        sessionRepository.getActiveUser().observeForever(activeUserObserver);
        User current = sessionRepository.getCurrentUser();
        if (current != null && current.nickname != null) {
            nickname.setValue(current.nickname);
        }
    }

    public LiveData<String> getNickname() {
        return nickname;
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
        if (value.length() < MIN_LEN || value.length() > MAX_LEN) {
            toastRes.setValue(R.string.account_error_nickname_length);
            return;
        }
        String current = nickname.getValue();
        if (value.equals(current)) {
            finishPage.call();
            return;
        }
        final int generation = updateGeneration.incrementAndGet();
        saving.setValue(true);
        sessionRepository.updateProfile(value, null, null, new AccountManager.ProfileUpdateCallback() {
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
