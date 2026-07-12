package com.skyinit.pomodorotimer.ui.consent;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.repository.PrivacyConsentRepository;

/**
 * 首次启动隐私与用户协议同意页 ViewModel。
 */
public class PrivacyConsentViewModel extends ViewModel {

    public enum ConsentAction {
        NAVIGATE_TO_MAIN,
        EXIT_APP
    }

    private final PrivacyConsentRepository repository;
    private final MutableLiveData<ConsentAction> action = new MutableLiveData<>();

    public PrivacyConsentViewModel(@NonNull PrivacyConsentRepository repository) {
        this.repository = repository;
    }

    public LiveData<ConsentAction> getAction() {
        return action;
    }

    public void onAcceptClicked() {
        repository.accept();
        action.setValue(ConsentAction.NAVIGATE_TO_MAIN);
    }

    public void onDeclineClicked() {
        action.setValue(ConsentAction.EXIT_APP);
    }
}
