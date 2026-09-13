package com.skyinit.pomodorotimer.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.data.entity.User;

/**
 * 可观察的用户会话层：Guest 时 activeUser 为 null。
 */
public final class UserSessionRepository {

    private final AccountManager accountManager;
    private final UserPomodoroSettingsRepository pomodoroSettingsRepository;
    private final UserAppBlockingRepository userAppBlockingRepository;
    private final AccountOperationGuard accountOperationGuard;
    private final MutableLiveData<User> activeUser = new MutableLiveData<>();
    private final MutableLiveData<Integer> sessionVersion = new MutableLiveData<>(0);

    public UserSessionRepository(AccountManager accountManager,
                                 UserPomodoroSettingsRepository pomodoroSettingsRepository,
                                 UserAppBlockingRepository userAppBlockingRepository,
                                 AccountOperationGuard accountOperationGuard) {
        this.accountManager = accountManager;
        this.pomodoroSettingsRepository = pomodoroSettingsRepository;
        this.userAppBlockingRepository = userAppBlockingRepository;
        this.accountOperationGuard = accountOperationGuard;
        accountManager.setActiveUserListener((user, sessionSwitched) -> {
            activeUser.postValue(user);
            if (sessionSwitched) {
                pomodoroSettingsRepository.clearCycleCountOnSessionSwitch();
                pomodoroSettingsRepository.invalidateCache();
                pomodoroSettingsRepository.warmCache();
                userAppBlockingRepository.clearCache();
                if (user != null) {
                    // Warm blocking cache asynchronously after switch
                    com.skyinit.pomodorotimer.util.AppExecutors.getInstance().diskIo(
                            () -> userAppBlockingRepository.warmForUser(user.userId));
                }
                accountOperationGuard.onActiveAccountSwitched();
                Integer current = sessionVersion.getValue();
                sessionVersion.postValue(current == null ? 1 : current + 1);
            }
        });
    }

    public void syncFromAccountManager() {
        activeUser.postValue(accountManager.getCurrentUser());
        User user = accountManager.getCurrentUser();
        if (user != null) {
            userAppBlockingRepository.warmForUser(user.userId);
        } else {
            userAppBlockingRepository.clearCache();
        }
    }

    public LiveData<User> getActiveUser() {
        return activeUser;
    }

    public LiveData<Integer> getSessionVersion() {
        return sessionVersion;
    }

    @NonNull
    public String requireActiveUserId() {
        return accountManager.requireActiveUserId();
    }

    @Nullable
    public User getCurrentUser() {
        return accountManager.getCurrentUser();
    }

    public boolean hasActiveSession() {
        return accountManager.hasActiveSession();
    }

    /** @deprecated 使用 {@link #hasActiveSession()} */
    @Deprecated
    public boolean hasActiveProfile() {
        return hasActiveSession();
    }

    public boolean isLoggedIn() {
        return accountManager.isLoggedIn();
    }

    /** @deprecated 使用 {@link #isLoggedIn()} */
    @Deprecated
    public boolean isRegistered() {
        return isLoggedIn();
    }

    public boolean isForcePasswordReset() {
        return accountManager.isForcePasswordReset();
    }

    public void clearForcePasswordReset() {
        accountManager.clearForcePasswordReset();
    }

    public void login(String userId, String password, AccountManager.LoginCallback callback) {
        accountManager.login(userId, password, callback);
    }

    public void recoverLogin(String userId, String nickname, AccountManager.LoginCallback callback) {
        accountManager.recoverLogin(userId, nickname, callback);
    }

    public void register(String nickname, String password, String confirmPassword,
                         String avatarPath, String signature, AccountManager.RegisterCallback callback) {
        accountManager.register(nickname, password, confirmPassword, avatarPath, signature, callback);
    }

    public void logout(AccountManager.LogoutCallback callback) {
        accountManager.logout(callback);
    }

    public void deleteCurrentAccount(AccountManager.AccountDeletionCallback callback) {
        accountManager.deleteCurrentAccount(callback);
    }

    public void updateProfile(String nickname, String avatarPath, String signature,
                              AccountManager.ProfileUpdateCallback callback) {
        accountManager.updateProfile(nickname, avatarPath, signature, callback);
    }

    public void updatePassword(String oldPassword, String newPassword, String confirmPassword,
                               AccountManager.PasswordUpdateCallback callback) {
        accountManager.updatePassword(oldPassword, newPassword, confirmPassword, callback);
    }
}
