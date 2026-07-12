package com.skyinit.pomodorotimer.ui.consent;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.data.repository.PrivacyConsentRepository;

public class PrivacyConsentViewModelFactory implements ViewModelProvider.Factory {

    private final PrivacyConsentRepository repository;

    public PrivacyConsentViewModelFactory(Context context) {
        repository = PrivacyConsentRepository.getInstance(context);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(PrivacyConsentViewModel.class)) {
            return (T) new PrivacyConsentViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
