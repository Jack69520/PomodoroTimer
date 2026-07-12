package com.skyinit.pomodorotimer.ui.profile;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.model.ProfileUiState;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;

/**
 * 个人页 ViewModel：可观察账户卡片、累计专注次数与应用屏蔽状态。
 */
public class ProfileViewModel extends ViewModel {

    private final Application application;
    private final UserSessionRepository sessionRepository;
    private final SettingsManager settingsManager;
    private final StatisticsRepository statisticsRepository;

    private final MutableLiveData<ProfileUiState> profileUiState = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalCompletedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> appBlockingEnabled = new MutableLiveData<>(false);

    private final Observer<User> activeUserObserver = this::onActiveUserChanged;
    private final Observer<Integer> sessionVersionObserver = unused -> loadStatistics();

    public ProfileViewModel(Application application,
                            UserSessionRepository sessionRepository,
                            SettingsManager settingsManager,
                            StatisticsRepository statisticsRepository) {
        this.application = application;
        this.sessionRepository = sessionRepository;
        this.settingsManager = settingsManager;
        this.statisticsRepository = statisticsRepository;

        sessionRepository.getActiveUser().observeForever(activeUserObserver);
        sessionRepository.getSessionVersion().observeForever(sessionVersionObserver);

        User current = sessionRepository.getCurrentUser();
        if (current != null) {
            onActiveUserChanged(current);
        }
        appBlockingEnabled.setValue(settingsManager.isAppBlockingEnabled());
        loadStatistics();
    }

    public LiveData<ProfileUiState> getProfileUiState() {
        return profileUiState;
    }

    public LiveData<Integer> getTotalCompletedCount() {
        return totalCompletedCount;
    }

    public LiveData<Boolean> isAppBlockingEnabled() {
        return appBlockingEnabled;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public void refresh() {
        appBlockingEnabled.setValue(settingsManager.isAppBlockingEnabled());
        loadStatistics();
    }

    public void setAppBlockingEnabled(boolean enabled) {
        settingsManager.setAppBlockingEnabled(enabled);
        appBlockingEnabled.setValue(enabled);
    }

    private void onActiveUserChanged(User user) {
        if (user == null) {
            profileUiState.setValue(null);
            totalCompletedCount.setValue(0);
            return;
        }
        profileUiState.setValue(mapProfileUiState(user));
        loadStatistics();
    }

    private void loadStatistics() {
        if (sessionRepository.hasActiveProfile()) {
            statisticsRepository.getTotalCompletedCount(totalCompletedCount::setValue);
        } else {
            totalCompletedCount.setValue(0);
        }
    }

    private ProfileUiState mapProfileUiState(User user) {
        String idLabel = user.isLocalProfile()
                ? application.getString(R.string.account_local_profile_label)
                : "ID: " + user.userId;
        boolean hasAvatar = user.avatarPath != null && !user.avatarPath.isEmpty();
        return new ProfileUiState(user.nickname, idLabel, user.avatarPath, hasAvatar);
    }

    @Override
    protected void onCleared() {
        sessionRepository.getActiveUser().removeObserver(activeUserObserver);
        sessionRepository.getSessionVersion().removeObserver(sessionVersionObserver);
        super.onCleared();
    }
}
